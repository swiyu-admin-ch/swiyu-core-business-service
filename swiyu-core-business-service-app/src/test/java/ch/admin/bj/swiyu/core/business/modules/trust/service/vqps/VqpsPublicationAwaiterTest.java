package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import static ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionStatusDto.ACCEPTED;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionStatusDto.PUBLICATION_SUCCEEDED;
import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.vqpsSubmissionB2BDto;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.exceptions.VqpsPublicationTimeoutException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VqpsPublicationAwaiterTest {

    @Mock
    private VqpsSubmissionService vqpsSubmissionService;

    private VqpsPublicationAwaiter awaiterWithMaxWaitTimeOf(long milliSeconds) {
        return new VqpsPublicationAwaiter(vqpsSubmissionService, Duration.ofMillis(milliSeconds));
    }

    @Test
    void notifyVqpsPublicationProcessFinished() {
        // GIVEN
        var awaiter = awaiterWithMaxWaitTimeOf(5000);
        var submissionId = awaiter.registerNewSubmission();
        var submission1 = vqpsSubmissionB2BDto(submissionId, ACCEPTED);
        var submission2 = vqpsSubmissionB2BDto(submissionId, PUBLICATION_SUCCEEDED);
        when(vqpsSubmissionService.getVqpsSubmissionB2B(submissionId))
            .thenReturn(submission1) // 1st call
            .thenReturn(submission2); // 2nd call

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            // WHEN
            executor.schedule(
                () -> awaiter.notifyVqpsPublicationProcessFinished(submissionId),
                100,
                TimeUnit.MILLISECONDS
            );

            // THEN
            var updatedSubmission = awaiter.waitForVqpsPublication(submissionId);
            assertThat(updatedSubmission.status()).isEqualTo(PUBLICATION_SUCCEEDED);
            executor.shutdown();
        }
    }

    @Test
    void notifyVqpsPublicationProcessFinished_whenNotificationArrivesBeforeWait_thenReturnsImmediately() {
        // Simulates the race condition: Kafka event is processed and notifyVqpsPublicationProcessFinished
        // is called BEFORE waitForVqpsPublication is invoked.
        // GIVEN
        var awaiter = awaiterWithMaxWaitTimeOf(5000);
        var submissionId = awaiter.registerNewSubmission();
        var succeededSubmission = vqpsSubmissionB2BDto(submissionId, PUBLICATION_SUCCEEDED);
        when(vqpsSubmissionService.getVqpsSubmissionB2B(submissionId)).thenReturn(succeededSubmission);

        // Simulate Kafka round-trip completing before waitForVqpsPublication is called
        awaiter.notifyVqpsPublicationProcessFinished(submissionId);

        // WHEN / THEN - must return immediately without blocking
        var updatedSubmission = awaiter.waitForVqpsPublication(submissionId);
        assertThat(updatedSubmission.status()).isEqualTo(PUBLICATION_SUCCEEDED);
    }

    @Test
    void notifyVqpsPublicationProcessFinished_whenNotNotifiedWithinTimeout_thenThrowsTimeoutException() {
        var awaiter = awaiterWithMaxWaitTimeOf(1);
        var submissionId = UUID.randomUUID();
        var submission = vqpsSubmissionB2BDto(submissionId, ACCEPTED);
        when(vqpsSubmissionService.getVqpsSubmissionB2B(submission.id())).thenReturn(submission);

        assertThatThrownBy(() -> awaiter.waitForVqpsPublication(submissionId))
            .isInstanceOf(VqpsPublicationTimeoutException.class)
            .extracting(e -> ((VqpsPublicationTimeoutException) e).getVqpsSubmissionId())
            .isEqualTo(submissionId);
    }

    @Test
    void notifyVqpsPublicationProcessFinished_whenNotifyCalledForUnknownSubmission_thenDoesNothing() {
        var awaiter = awaiterWithMaxWaitTimeOf(1);
        assertThatNoException().isThrownBy(() -> awaiter.notifyVqpsPublicationProcessFinished(UUID.randomUUID()));
    }
}
