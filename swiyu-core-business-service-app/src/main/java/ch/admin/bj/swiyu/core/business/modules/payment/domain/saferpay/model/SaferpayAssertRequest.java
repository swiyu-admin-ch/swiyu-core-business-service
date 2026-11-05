package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body of {@code POST /Payment/v1/PaymentPage/Assert}.
 * See https://saferpay.github.io/jsonapi/#Payment_v1_PaymentPage_Assert
 */
public record SaferpayAssertRequest(
    @JsonProperty("RequestHeader") SaferpayRequestHeader requestHeader,
    @JsonProperty("Token") String token
) {}
