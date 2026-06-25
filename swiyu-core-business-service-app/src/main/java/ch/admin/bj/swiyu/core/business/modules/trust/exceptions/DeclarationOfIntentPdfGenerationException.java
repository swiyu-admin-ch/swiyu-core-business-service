package ch.admin.bj.swiyu.core.business.modules.trust.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import jakarta.annotation.Nullable;
import java.text.MessageFormat;
import lombok.Getter;

public class DeclarationOfIntentPdfGenerationException extends BusinessException {

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "Declaration of intent PDF could not be generated. {0}"
    );

    @Getter
    private final BusinessExceptionErrorCode errorCode = BusinessExceptionErrorCode.DATA_INVALID;

    public DeclarationOfIntentPdfGenerationException(String additionalDetails) {
        super(RESOURCE_TYPE_MESSAGE.format(new Object[] { additionalDetails }), additionalDetails, null);
    }

    public DeclarationOfIntentPdfGenerationException(String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new Object[] { additionalDetails }), additionalDetails, cause);
    }
}
