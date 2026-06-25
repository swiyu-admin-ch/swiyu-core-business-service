package ch.admin.bj.swiyu.core.business.modules.trust.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;

public class TrustOnboardingSubmissionDocumentValidationError extends BusinessException {

    public TrustOnboardingSubmissionDocumentValidationError(String additionalDetails) {
        super("TrustOnboardingSubmissionDocument '%s' could not be validated.", additionalDetails, null);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.TRUST_ONBOARDING_DOCUMENT_VALIDATION_FAILED;
    }
}
