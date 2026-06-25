package ch.admin.bj.swiyu.core.business.modules.identifier.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import jakarta.annotation.Nullable;
import java.text.MessageFormat;
import java.util.List;

public class IdentifierValidationFailedException extends BusinessException {

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "Provided identifier resource is invalid."
    );

    public IdentifierValidationFailedException(String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new String[] {}), additionalDetails, cause);
    }

    public IdentifierValidationFailedException(List<String> additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new String[] {}), additionalDetails, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.IDENTIFIER_VALIDATION_FAILED;
    }
}
