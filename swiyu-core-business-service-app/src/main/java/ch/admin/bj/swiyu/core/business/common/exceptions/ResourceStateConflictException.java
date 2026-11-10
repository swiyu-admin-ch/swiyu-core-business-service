package ch.admin.bj.swiyu.core.business.common.exceptions;

/**
 * A request conflicts with the current state of the resource, e.g. retrying a payment that is still
 * pending or already succeeded. Answered with {@code 409 Conflict} by {@code RestExceptionHandler}.
 *
 * Subclasses should override {@link #getErrorCode()} with a specific error code, so the caller can tell
 * the conflicts apart.
 */
public class ResourceStateConflictException extends BusinessException {

    public ResourceStateConflictException(String message) {
        super(message);
    }

    public ResourceStateConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.DATA_INVALID;
    }
}
