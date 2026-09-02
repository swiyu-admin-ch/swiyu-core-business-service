package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionValidatorErrorCodeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrustOnboardingSubmissionValidatorTest {

    private final TrustOnboardingSubmissionValidator validator = new TrustOnboardingSubmissionValidator();

    @Test
    void allows_editing_when_unsubmitted() {
        var submission = trustOnboardingSubmission(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustOnboardingSubmissionStatus.UNSUBMITTED,
            Instant.now()
        );

        var errors = validator.validateTrustOnboardingSubmissionCanBeEdited(submission, null);

        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void allows_editing_when_information_requested_without_deadline() {
        var submission = trustOnboardingSubmission(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED,
            Instant.now()
        );

        var errors = validator.validateTrustOnboardingSubmissionCanBeEdited(submission, null);

        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void blocks_editing_when_resubmitted() {
        var submission = trustOnboardingSubmission(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustOnboardingSubmissionStatus.RESUBMITTED,
            Instant.now()
        );
        submission.markAsResubmitted(Instant.now().plus(7, ChronoUnit.DAYS), "please adjust");

        var errors = validator.validateTrustOnboardingSubmissionCanBeEdited(submission, null);

        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.getGlobalError().getCode()).isEqualTo(
            TrustOnboardingSubmissionValidatorErrorCodeDto.EDITING_BLOCKED.toString()
        );
    }

    @Test
    void blocks_editing_when_status_not_editable() {
        var submission = trustOnboardingSubmission(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustOnboardingSubmissionStatus.SUBMITTED,
            Instant.now()
        );

        var errors = validator.validateTrustOnboardingSubmissionCanBeEdited(submission, null);

        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.getGlobalError().getCode()).isEqualTo(
            TrustOnboardingSubmissionValidatorErrorCodeDto.EDITING_BLOCKED.toString()
        );
    }

    @Test
    void blocks_editing_when_information_requested_and_resubmission_deadline_expired() {
        var submission = trustOnboardingSubmission(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED,
            Instant.now()
        );
        submission.markAsInformationRequested(Instant.now().minus(1, ChronoUnit.HOURS), "please adjust");

        var errors = validator.validateTrustOnboardingSubmissionCanBeEdited(submission, null);

        assertThat(errors.hasErrors()).isTrue();
        assertThat(errors.getGlobalError().getCode()).isEqualTo(
            TrustOnboardingSubmissionValidatorErrorCodeDto.SUBMISSION_DEADLINE_EXPIRED.toString()
        );
    }
}
