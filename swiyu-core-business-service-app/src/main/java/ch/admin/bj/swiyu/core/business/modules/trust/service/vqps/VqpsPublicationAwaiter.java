package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import static java.lang.Thread.sleep;

import ch.admin.bj.swiyu.core.business.common.exceptions.VqpsPublicationFailedException;
import ch.admin.bj.swiyu.core.business.common.exceptions.VqpsPublicationTimeoutException;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionB2BDto;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VqpsPublicationAwaiter {

    /**
     * Maximum time to wait for the publication process to finish before timing out.
     */
    private static final Duration DEFAULT_MAX_WAIT_TIME = Duration.ofSeconds(10);

    /**
     * How long to sleep between DB polls.
     */
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(200);

    private final VqpsSubmissionService submissionService;
    private final Duration maxWaitTime;
    private final Duration pollInterval;

    @Autowired
    public VqpsPublicationAwaiter(VqpsSubmissionService submissionService) {
        this(submissionService, DEFAULT_MAX_WAIT_TIME, DEFAULT_POLL_INTERVAL);
    }

    @VisibleForTesting
    VqpsPublicationAwaiter(VqpsSubmissionService submissionService, Duration maxWaitTime) {
        this(submissionService, maxWaitTime, DEFAULT_POLL_INTERVAL);
    }

    @VisibleForTesting
    VqpsPublicationAwaiter(VqpsSubmissionService submissionService, Duration maxWaitTime, Duration pollInterval) {
        this.submissionService = submissionService;
        this.maxWaitTime = maxWaitTime;
        this.pollInterval = pollInterval;
    }

    /**
     * Polls the database until the submission reaches a terminal state or {@code maxWaitTime} elapses.
     */
    @SuppressWarnings("BusyWait") // since we use virtual threads
    public VqpsSubmissionB2BDto waitForVqpsPublication(UUID submissionId) {
        log.debug("Waiting for VQPS publication to finish by polling db for for submission id {}", submissionId);

        var deadline = Instant.now().plus(maxWaitTime);

        while (true) {
            var submission = submissionService.getVqpsSubmissionB2B(submissionId);
            if (submission.isSucceeded()) {
                return submission;
            } else if (submission.isFailed()) {
                return throwPublicationFailedException(submission);
            }

            if (Instant.now().isAfter(deadline)) {
                throw new VqpsPublicationTimeoutException(submissionId);
            }

            try {
                sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for VQPS publication", e);
            }
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
