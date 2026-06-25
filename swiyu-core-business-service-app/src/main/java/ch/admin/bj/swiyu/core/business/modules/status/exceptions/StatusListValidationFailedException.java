package ch.admin.bj.swiyu.core.business.modules.status.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import ch.admin.bj.swiyu.core.business.common.exceptions.ExposeAdditionalDetailsBusinessException;
import jakarta.annotation.Nullable;
import java.text.MessageFormat;

public class StatusListValidationFailedException
    extends BusinessException
    implements ExposeAdditionalDetailsBusinessException
{

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "Provided status list resource is invalid."
    );

    public StatusListValidationFailedException(String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new String[] {}), additionalDetails, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.STATUS_LIST_VALIDATION_FAILED;
    }
}
