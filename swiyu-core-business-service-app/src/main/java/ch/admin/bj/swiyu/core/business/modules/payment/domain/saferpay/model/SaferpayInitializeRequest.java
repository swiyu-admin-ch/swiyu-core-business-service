package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /Payment/v1/PaymentPage/Initialize}.
 * See https://saferpay.github.io/jsonapi/#Payment_v1_PaymentPage_Initialize
 */
public record SaferpayInitializeRequest(
    @JsonProperty("RequestHeader") SaferpayRequestHeader requestHeader,
    @JsonProperty("TerminalId") String terminalId,
    @JsonProperty("Payment") SaferpayPayment payment,
    @JsonProperty("ConfigSet") @JsonInclude(JsonInclude.Include.NON_EMPTY) String configSet,
    @JsonProperty("ReturnUrl") SaferpayReturnUrl returnUrl
) {}
