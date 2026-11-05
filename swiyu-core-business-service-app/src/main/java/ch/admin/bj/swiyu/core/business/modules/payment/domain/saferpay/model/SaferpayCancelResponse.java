package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body of {@code POST /Payment/v1/Transaction/Cancel}.
 * See https://saferpay.github.io/jsonapi/#Payment_v1_Transaction_Cancel
 */
public record SaferpayCancelResponse(
    @JsonProperty("ResponseHeader") SaferpayResponseHeader responseHeader,
    @JsonProperty("Transaction") SaferpayTransaction transaction
) {}
