package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import ch.admin.bj.swiyu.core.business.common.exceptions.VqpsPublicationFailedException;
import ch.admin.bj.swiyu.core.business.common.exceptions.VqpsPublicationTimeoutException;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionB2BDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VqpsPublicationAwaiter {

    /**
     * Maximum Time to wait for the publication process to finish before timing out.
     */
    private static final Duration DEFAULT_MAX_WAIT_TIME = Duration.ofSeconds(10);
    private static final Duration AUTO_REMOVE_PENDING_REQUESTS_DURATION = Duration.ofSeconds(2);

    /**
     * Keeps for each incoming VqpsSubmission (its id) the completable future in cache with a Time-To-Live
     * defined in AUTO_REMOVE_PENDING_REQUESTS_DURATION.
     */
    private final Cache<UUID, CompletableFuture<Void>> pendingRequests;
    private final VqpsSubmissionService submissionService;
    private final Duration maxWaitTime;

    @Autowired
    public VqpsPublicationAwaiter(VqpsSubmissionService submissionService) {
        this(submissionService, DEFAULT_MAX_WAIT_TIME);
    }

    @VisibleForTesting
    VqpsPublicationAwaiter(VqpsSubmissionService submissionService, Duration maxWaitTime) {
        this.submissionService = submissionService;
        this.maxWaitTime = maxWaitTime;
        // auto remove pending requests that are over a certian wait time
        this.pendingRequests = Caffeine.newBuilder().expireAfterWrite(AUTO_REMOVE_PENDING_REQUESTS_DURATION).build();
    }

    public VqpsSubmissionB2BDto waitForVqpsPublication(UUID submissionId) {
        log.debug("Waiting for VQPS publication to finish for submission id {}", submissionId);
        // 1/2: check submission already completed, if so directly return the result
        var submission = submissionService.getVqpsSubmissionB2B(submissionId);
        if (submission.isSucceeded()) {
            return submission;
        } else if (submission.isFailed()) {
            throwPublicationFailedException(submission);
        }

        // 2/2: not yet completed -> wait until handled
        var future = new CompletableFuture<Void>();
        pendingRequests.put(submissionId, future);
        try {
            future.get(maxWaitTime.toNanos(), TimeUnit.NANOSECONDS);
            log.debug(
                "Finished waiting for VQPS publication for submission id {}. Fetching final submission state...",
                submissionId
            );
            submission = submissionService.getVqpsSubmissionB2B(submissionId);
            return switch (submission.status()) {
                case PUBLICATION_SUCCEEDED -> submission;
                case PUBLICATION_FAILED -> throwPublicationFailedException(submission);
                case ACCEPTED -> throw new IllegalStateException(
                    "The publication finished but status is still ACCEPTED"
                );
            };
        } catch (TimeoutException e) {
            throw new VqpsPublicationTimeoutException(submissionId, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unexpected execution exception while waiting for VQPS publication", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unexpected interrupt exception while waiting for VQPS publication", e);
        } finally {
            pendingRequests.invalidate(submissionId);
        }
    }

    /**
     * Called by VqpsPublicationEventProcessor after the transaction has committed
     */
    public void notifyVqpsPublicationProcessFinished(UUID vqpsSubmissionId) {
        var future = pendingRequests.getIfPresent(vqpsSubmissionId);
        if (future != null) {
            pendingRequests.invalidate(vqpsSubmissionId);
            future.complete(null);
        }
    }

    private VqpsSubmissionB2BDto throwPublicationFailedException(VqpsSubmissionB2BDto submission) {
        throw new VqpsPublicationFailedException(
            "VQPS publication failed for submission %s with reason %s".formatted(
                submission.id(),
                submission.publicationFailureReason()
            ),
            submission.id()
        );
    }
}
