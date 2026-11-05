package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body of {@code POST /Payment/v1/PaymentPage/Assert}.
 * See https://saferpay.github.io/jsonapi/#Payment_v1_PaymentPage_Assert
 */
public record SaferpayAssertResponse(
    @JsonProperty("ResponseHeader") SaferpayResponseHeader responseHeader,
    @JsonProperty("Transaction") SaferpayTransaction transaction,
    @JsonProperty("Liability") SaferpayLiability liability
) {}
