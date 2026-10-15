package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

/**
 * Input for {@link SaferpayClient#initialize(SaferpayInitializePaymentCommand)}.
 *
 * @param orderId     the Payment's referenceId, shown as "orderId" in Saferpay's UI
 * @param amountValue minor units (e.g. Rappen for CHF)
 * @param currency    ISO 4217 currency code, e.g. "CHF"
 * @param description shown on the Saferpay hosted payment page
 * @param returnUrl   URL the browser is redirected back to after payment
 */
public record SaferpayInitializePaymentCommand(
    String orderId,
    long amountValue,
    String currency,
    String description,
    String returnUrl
) {}
