package ch.admin.bj.swiyu.core.business.common.exceptions;

import java.text.MessageFormat;
import org.springframework.util.unit.DataSize;

public class MaxSizeApiException extends BusinessException {

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat(
        "Uploaded data exceeds the maximum size."
    );
    private static final MessageFormat DETAIL_MESSAGE = new MessageFormat(
        "Resource {0} has size {1} but should not exceed {2}."
    );

    public MaxSizeApiException(DataSize maxSize, int currentByteSize, String offendingResource) {
        this(maxSize, DataSize.ofBytes(currentByteSize), offendingResource);
    }

    public MaxSizeApiException(DataSize maxSize, DataSize currentSize, String offendingResource) {
        super(
            RESOURCE_TYPE_MESSAGE.format(new Object[] {}),
            DETAIL_MESSAGE.format(new Object[] { offendingResource, currentSize.toString(), maxSize.toString() }),
            null
        );
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.MAX_SIZE_EXCEEDED;
    }
}
