package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
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

    /**
     * A submission may only be changed while it waits for the partner: UNSUBMITTED, or INFORMATION_REQUESTED before the
     * deadline to answer. Every state a partner handed the submission over in (SUBMITTED, RESUBMITTED) and every state
     * a trust officer decided on is final.
     */
    public void validateCanEdit(TrustOnboardingSubmission submission) {
        var errors = validateTrustOnboardingSubmissionCanBeEdited(submission, null);
        if (errors.hasErrors()) {
            throw new ValidationException("Trust onboarding submission can no longer be edited", errors);
        }
    }

    /**
     * A submission may confirm the partner type of its partner but never switch it: a registration or profile change of
     * a governmental partner stays governmental, one of a business partner stays business. The rule lives here and
     * nowhere else, so a later relaxation for another partner type is a single edit.
     */
    public Errors validateRequestedPartnerTypeChange(
        BusinessPartnerType requestedPartnerType,
        BusinessPartnerType currentPartnerType,
        @Nullable Errors errors
    ) {
        if (errors == null) {
            errors = new SimpleErrors(requestedPartnerType);
        }
        if (requestedPartnerType != currentPartnerType) {
            errors.reject(
                TrustOnboardingSubmissionValidatorErrorCodeDto.PARTNER_TYPE_CHANGE_NOT_ALLOWED.toString(),
                "Requested partner type '%s' does not match the partner type '%s' of the business partner".formatted(
                    requestedPartnerType,
                    currentPartnerType
                )
            );
        }
        return errors;
    }

    public void validateCanDelete(TrustOnboardingSubmission submission) {
        if (submission.isRenewal()) {
            throw new ValidationException(
                "Documents cannot be deleted from a RENEWAL submission.",
                new BeanPropertyBindingResult(submission, "trustOnboardingSubmission")
            );
        }
        validateCanEdit(submission);
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
