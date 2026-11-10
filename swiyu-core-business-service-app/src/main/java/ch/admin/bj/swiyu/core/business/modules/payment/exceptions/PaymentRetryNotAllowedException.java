package ch.admin.bj.swiyu.core.business.modules.payment.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceStateConflictException;

/**
 * A payment cannot be retried because it is neither FAILED nor CANCELLED, e.g. because it is still
 * PENDING on the Saferpay payment page or because it has already been paid. Answered with
 * {@code 409 Conflict} by {@code RestExceptionHandler}.
 */
public class PaymentRetryNotAllowedException extends ResourceStateConflictException {

    public PaymentRetryNotAllowedException(String message) {
        super(message);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.PAYMENT_NOT_RETRYABLE;
    }
}
