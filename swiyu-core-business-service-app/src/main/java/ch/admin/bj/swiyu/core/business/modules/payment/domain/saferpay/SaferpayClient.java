package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

public interface SaferpayClient {
    SaferpayInitializeResult initialize(SaferpayInitializePaymentCommand command);
}
