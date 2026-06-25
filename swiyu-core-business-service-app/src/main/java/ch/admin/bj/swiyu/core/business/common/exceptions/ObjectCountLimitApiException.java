package ch.admin.bj.swiyu.core.business.common.exceptions;

import java.text.MessageFormat;

public class ObjectCountLimitApiException extends BusinessException {

    private static final String RESOURCE_TYPE_MESSAGE = "Object count limit reached.";
    private static final String TEMPLATE_DETAIL_MESSAGE = "Resources belonging to {0} has a maximum count of {1}.";

    public ObjectCountLimitApiException(String objectType, long offendingCountLimit) {
        super(
            RESOURCE_TYPE_MESSAGE,
            MessageFormat.format(TEMPLATE_DETAIL_MESSAGE, objectType, offendingCountLimit),
            null
        );
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.OBJECT_COUNT_LIMIT_REACHED;
    }
}
