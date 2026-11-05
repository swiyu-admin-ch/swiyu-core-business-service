package ch.admin.bj.swiyu.core.business.modules.payment.exceptions;

public class PaymentFinalizationFailedException extends RuntimeException {

    public PaymentFinalizationFailedException(String message) {
        super(message);
    }

    public PaymentFinalizationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
