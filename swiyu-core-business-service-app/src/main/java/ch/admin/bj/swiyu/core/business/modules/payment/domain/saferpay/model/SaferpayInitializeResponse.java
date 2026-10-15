package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body of {@code POST /Payment/v1/PaymentPage/Initialize}.
 * See https://saferpay.github.io/jsonapi/#Payment_v1_PaymentPage_Initialize
 *
 * @param token       handle for the payment page session, needed later for PaymentPage/Assert
 * @param redirectUrl URL of the payment page the browser has to be sent to
 * @param expiration  ISO-8601 timestamp until which the token is valid
 */
public record SaferpayInitializeResponse(
    @JsonProperty("Token") String token,
    @JsonProperty("RedirectUrl") String redirectUrl,
    @JsonProperty("Expiration") String expiration
) {}
