package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transaction object in Saferpay responses.
 * See https://saferpay.github.io/jsonapi/#Payment_v1_PaymentPage_Assert
 */
public record SaferpayTransaction(
    @JsonProperty("TransactionId") String transactionId,
    @JsonProperty("Status") String status,
    @JsonProperty("Type") String type,
    @JsonProperty("Amount") SaferpayAmount amount,
    @JsonProperty("CurrencyCode") String currencyCode,
    @JsonProperty("Date") String date,
    @JsonProperty("OrderId") String orderId,
    @JsonProperty("AcquirerName") String acquirerName,
    @JsonProperty("PaymentMeans") SaferpayPaymentMeans paymentMeans,
    @JsonProperty("TransactionReference") SaferpayTransactionReference transactionReference
) {}
