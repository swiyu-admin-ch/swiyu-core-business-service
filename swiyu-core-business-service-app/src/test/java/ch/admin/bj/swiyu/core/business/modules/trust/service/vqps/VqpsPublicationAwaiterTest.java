package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import static ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionStatusDto.ACCEPTED;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionStatusDto.PUBLICATION_SUCCEEDED;
import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.vqpsSubmissionB2BDto;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.exceptions.VqpsPublicationTimeoutException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VqpsPublicationAwaiterTest {

    @Mock
    private VqpsSubmissionService vqpsSubmissionService;

    @Test
    void waitForVqpsPublication() {
        // GIVEN
        var maxWaitTime = Duration.ofMillis(100);
        var pollIntervall = Duration.ofMillis(10);
        var awaiter = awaiterWithMaxWaitTimeOf(maxWaitTime, pollIntervall);
        var submissionId = UUID.randomUUID();
        var acceptedSubmission = vqpsSubmissionB2BDto(submissionId, ACCEPTED);
        var succeededSubmission = vqpsSubmissionB2BDto(submissionId, PUBLICATION_SUCCEEDED);
        when(vqpsSubmissionService.getVqpsSubmissionB2B(submissionId))
            .thenReturn(acceptedSubmission) // initial check still pending
            .thenReturn(acceptedSubmission) // poll 1
            .thenReturn(succeededSubmission); // poll 2 - DB updated by other pod

        // WHEN
        var result = awaiter.waitForVqpsPublication(submissionId);

        // THEN
        assertThat(result.status()).isEqualTo(PUBLICATION_SUCCEEDED);
    }

    @Test
    void waitForVqpsPublication_whenTimedOut() {
        var veryShortWaitTime = Duration.ofMillis(2);
        var pollIntervall = Duration.ofMillis(1);
        var awaiter = awaiterWithMaxWaitTimeOf(veryShortWaitTime, pollIntervall);
        var submissionId = UUID.randomUUID();
        var submission = vqpsSubmissionB2BDto(submissionId, ACCEPTED);
        when(vqpsSubmissionService.getVqpsSubmissionB2B(submission.id())).thenReturn(submission);

        assertThatThrownBy(() -> awaiter.waitForVqpsPublication(submissionId))
            .isInstanceOf(VqpsPublicationTimeoutException.class)
            .extracting(e -> ((VqpsPublicationTimeoutException) e).getVqpsSubmissionId())
            .isEqualTo(submissionId);
    }

    private VqpsPublicationAwaiter awaiterWithMaxWaitTimeOf(Duration maxWaitTime, Duration pollInterval) {
        return new VqpsPublicationAwaiter(vqpsSubmissionService, maxWaitTime, pollInterval);
    }
}
