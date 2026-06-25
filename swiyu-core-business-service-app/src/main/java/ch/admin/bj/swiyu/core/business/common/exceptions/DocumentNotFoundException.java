package ch.admin.bj.swiyu.core.business.common.exceptions;

import jakarta.annotation.Nullable;
import java.text.MessageFormat;

public class DocumentNotFoundException extends BusinessException {

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "Document data could not be found. {0}"
    );

    public DocumentNotFoundException(String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new Object[] { additionalDetails }), cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.DOCUMENT_NOT_FOUND;
    }
}
