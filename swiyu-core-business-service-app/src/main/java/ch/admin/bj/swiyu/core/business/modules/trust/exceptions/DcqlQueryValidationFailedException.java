package ch.admin.bj.swiyu.core.business.modules.trust.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import jakarta.annotation.Nullable;
import java.text.MessageFormat;

public class DcqlQueryValidationFailedException extends BusinessException {

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "vqPS submission request is invalid. {0}"
    );

    public DcqlQueryValidationFailedException(String additionalDetails) {
        super(RESOURCE_TYPE_MESSAGE.format(new Object[] { additionalDetails }), additionalDetails, null);
    }

    public DcqlQueryValidationFailedException(String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new Object[] { additionalDetails }), additionalDetails, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.DATA_INVALID;
    }
}
