package ch.admin.bj.swiyu.core.business.common.exceptions;

/**
 * Exception thrown when a payment receipt cannot be generated because the payment is not in SUCCEEDED state.
 */
public class PaymentReceiptNotAvailableException extends BusinessException {

    public PaymentReceiptNotAvailableException(String message) {
        super(message);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.PAYMENT_RECEIPT_NOT_AVAILABLE;
    }
}
