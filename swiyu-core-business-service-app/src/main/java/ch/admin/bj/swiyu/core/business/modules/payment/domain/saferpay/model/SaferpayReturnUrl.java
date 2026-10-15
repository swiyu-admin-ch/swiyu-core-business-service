package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SaferpayReturnUrl(@JsonProperty("Url") String url) {}
