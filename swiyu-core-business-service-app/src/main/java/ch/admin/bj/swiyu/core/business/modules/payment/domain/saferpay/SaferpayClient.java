package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

public interface SaferpayClient {
    SaferpayInitializeResult initialize(SaferpayInitializePaymentCommand command);

    SaferpayAssertResult assertPayment(SaferpayAssertCommand command);

    SaferpayCaptureResult captureTransaction(SaferpayCaptureCommand command);

    SaferpayCancelResult cancelTransaction(SaferpayCancelCommand command);
}
