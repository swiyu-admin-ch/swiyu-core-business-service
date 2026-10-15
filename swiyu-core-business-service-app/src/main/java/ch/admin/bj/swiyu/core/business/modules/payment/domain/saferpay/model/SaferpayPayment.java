package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SaferpayPayment(
    @JsonProperty("Amount") SaferpayAmount amount,
    @JsonProperty("OrderId") String orderId,
    @JsonProperty("Description") String description
) {}
