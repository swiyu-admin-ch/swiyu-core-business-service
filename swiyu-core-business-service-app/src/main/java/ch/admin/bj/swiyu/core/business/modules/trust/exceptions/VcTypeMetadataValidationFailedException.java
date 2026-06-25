package ch.admin.bj.swiyu.core.business.modules.trust.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import jakarta.annotation.Nullable;
import java.text.MessageFormat;

public class VcTypeMetadataValidationFailedException extends BusinessException {

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "Provided VcMetadataType resource is invalid."
    );

    public VcTypeMetadataValidationFailedException(String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new String[] {}), additionalDetails, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.VC_TYPE_METADATA_VALIDATION_FAILED;
    }
}
