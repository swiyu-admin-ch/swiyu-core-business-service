package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import ch.admin.bj.swiyu.core.business.common.exceptions.ValidationException;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionValidatorErrorCodeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SimpleErrors;

@Component
@RequiredArgsConstructor
public class TrustOnboardingSubmissionValidator {

    public void validateCanDelete(TrustOnboardingSubmission submission) {
        if (submission.isRenewal()) {
            throw new ValidationException(
                "Documents cannot be deleted from a RENEWAL submission.",
                new BeanPropertyBindingResult(submission, "trustOnboardingSubmission")
            );
        }
        var errors = validateTrustOnboardingSubmissionCanBeEdited(submission, null);
        if (errors.hasErrors()) {
            throw new ValidationException("Submission cannot be edited.", errors);
        }
    }

    public Errors validateTrustOnboardingSubmissionCanBeEdited(
        TrustOnboardingSubmission submission,
        @Nullable Errors errors
    ) {
        if (errors == null) {
            errors = new SimpleErrors(submission);
        }
        if (
            !EnumSet.of(
                TrustOnboardingSubmissionStatus.UNSUBMITTED,
                TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
            ).contains(submission.getStatus())
        ) {
            errors.reject(
                TrustOnboardingSubmissionValidatorErrorCodeDto.EDITING_BLOCKED.toString(),
                "Submission is in state '%s' and cannot be edited right now".formatted(submission.getStatus())
            );
        } else if (isResubmissionDeadlineExpired(submission)) {
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
