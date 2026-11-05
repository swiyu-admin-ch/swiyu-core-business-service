package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

/**
 * Saferpay transaction status constants.
 * See https://saferpay.github.io/jsonapi/#Payment_v1_PaymentPage_Assert
 */
public final class SaferpayTransactionStatus {

    private SaferpayTransactionStatus() {}

    public static final String AUTHORIZED = "AUTHORIZED";
    public static final String CAPTURED = "CAPTURED";
    public static final String CANCELLED = "CANCELLED";
    public static final String FAILED = "FAILED";
    public static final String PENDING = "PENDING";
}
