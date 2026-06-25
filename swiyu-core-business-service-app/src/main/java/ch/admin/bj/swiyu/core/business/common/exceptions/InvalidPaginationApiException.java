package ch.admin.bj.swiyu.core.business.common.exceptions;

import jakarta.annotation.Nullable;
import java.text.MessageFormat;

public class InvalidPaginationApiException extends BusinessException {

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "Pagination data for resource {0} is invalid. {1}"
    );

    public InvalidPaginationApiException(Class<?> resourceType, String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new String[] { resourceType.getSimpleName(), additionalDetails }), cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.INVALID_PAGINATION;
    }
}
