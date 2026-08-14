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

    /**
     * Keeps for each incoming VqpsSubmission (its id) the completable future in cache with a Time-To-Live
     * slightly longer than the max wait time. This ensures entries are not evicted while they are still
     * being waited on, while still providing an automatic cleanup safety net.
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
        // TTL must exceed maxWaitTime so futures are not evicted before the wait completes
        this.pendingRequests = Caffeine.newBuilder().expireAfterWrite(maxWaitTime.plus(Duration.ofSeconds(5))).build();
    }

    /**
     * Pre-registers a pending future for the given submission ID. Must be called <em>before</em>
     * {@link VqpsSubmissionService#createVqpsSubmission} so that the future is in place before
     * the Kafka event can be published and the notification can arrive.
     *
     * @return the submission ID for which the future was registered
     */
    public UUID registerNewSubmission() {
        var submissionId = UUID.randomUUID();
        pendingRequests.put(submissionId, new CompletableFuture<>());
        return submissionId;
    }

    public VqpsSubmissionB2BDto waitForVqpsPublication(UUID submissionId) {
        log.debug("Waiting for VQPS publication to finish for submission id {}", submissionId);

        // Retrieve the pre-registered future. Fall back to creating a new one only when
        // waitForVqpsPublication is called without a prior registerPendingSubmission call.
        var future = pendingRequests.get(submissionId, ignored -> new CompletableFuture<>());

        // Check whether publication already completed before we started waiting.
        // If notifyVqpsPublicationProcessFinished was already called, the future is already
        // completed so future.get() will return immediately below.
        var submission = submissionService.getVqpsSubmissionB2B(submissionId);
        if (submission.isSucceeded()) {
            pendingRequests.invalidate(submissionId);
            return submission;
        } else if (submission.isFailed()) {
            pendingRequests.invalidate(submissionId);
            throwPublicationFailedException(submission);
        }

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
            // Complete the future but do NOT invalidate here: waitForVqpsPublication still holds a
            // reference and will invalidate after it reads the result.
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
