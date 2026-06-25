package ch.admin.bj.swiyu.core.business.common.exceptions;

public class BusinessDataIntegrityViolationException extends BusinessException {

    public BusinessDataIntegrityViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.BUSINESS_DATA_INTEGRITY_VIOLATION;
    }
}
