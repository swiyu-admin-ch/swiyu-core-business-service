package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionValidatorErrorCodeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.SimpleErrors;

@Component
@RequiredArgsConstructor
public class TrustOnboardingSubmissionValidator {

    public Errors validateTrustOnboardingSubmissionCanBeEdited(
        TrustOnboardingSubmission trustOnboarding,
        @Nullable Errors errors
    ) {
        if (errors == null) {
            errors = new SimpleErrors(trustOnboarding);
        }
        if (
            !EnumSet.of(
                TrustOnboardingSubmissionStatus.UNSUBMITTED,
                TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
            ).contains(trustOnboarding.getStatus())
        ) {
            errors.reject(
                TrustOnboardingSubmissionValidatorErrorCodeDto.EDITING_BLOCKED.toString(),
                "Submission is in state '%s' and cannot be edited right now".formatted(trustOnboarding.getStatus())
            );
        } else if (isResubmissionDeadlineExpired(trustOnboarding)) {
            errors.reject(
                TrustOnboardingSubmissionValidatorErrorCodeDto.SUBMISSION_DEADLINE_EXPIRED.toString(),
                "The deadline to resubmit the submission has passed and it can no longer be edited"
            );
        }
        return errors;
    }

    private boolean isResubmissionDeadlineExpired(TrustOnboardingSubmission trustOnboarding) {
        var resubmitRequiredUntil = trustOnboarding.getResubmitRequiredUntil();
        return resubmitRequiredUntil != null && resubmitRequiredUntil.isBefore(Instant.now());
    }
}
