package ch.admin.bj.swiyu.core.business.common.exceptions;

import jakarta.annotation.Nullable;
import java.text.MessageFormat;

public class DidResolveException extends BusinessException {

    private static final MessageFormat RESOURCE_TYPE_MESSAGE = new MessageFormat("Could not resolve DID ({0}).");

    public DidResolveException(String did, String additionalDetails, @Nullable Throwable cause) {
        super(RESOURCE_TYPE_MESSAGE.format(new String[] { did }), additionalDetails, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.DID_RESOLVE_FAILED;
    }
}
