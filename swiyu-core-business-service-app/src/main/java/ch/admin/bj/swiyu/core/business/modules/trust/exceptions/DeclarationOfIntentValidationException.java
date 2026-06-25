package ch.admin.bj.swiyu.core.business.modules.trust.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import java.util.List;

public class DeclarationOfIntentValidationException extends BusinessException {

    public DeclarationOfIntentValidationException(List<String> violationCodes) {
        super("Declaration of intent validation failed.", violationCodes, null);
    }

    public DeclarationOfIntentValidationException(List<String> violationCodes, Throwable cause) {
        super("Declaration of intent validation failed.", violationCodes, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.TRUST_ONBOARDING_DOCUMENT_VALIDATION_FAILED;
    }
}
