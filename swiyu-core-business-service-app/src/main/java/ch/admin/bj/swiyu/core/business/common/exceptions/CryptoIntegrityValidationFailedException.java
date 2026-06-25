package ch.admin.bj.swiyu.core.business.common.exceptions;

import jakarta.annotation.Nullable;
import java.text.MessageFormat;

public class CryptoIntegrityValidationFailedException
    extends BusinessException
    implements ExposeAdditionalDetailsBusinessException
{

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "Cryptographically integrity of the jwt is invalid. {0}"
    );

    public CryptoIntegrityValidationFailedException(String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new String[] { additionalDetails }), additionalDetails, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.CRYPTO_INTEGRITY_VALIDATION_FAILED;
    }
}
