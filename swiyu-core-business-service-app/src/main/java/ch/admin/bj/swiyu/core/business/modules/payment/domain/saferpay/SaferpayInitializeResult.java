package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

import java.time.Instant;

/**
 * Result of opening a Saferpay payment page session.
 *
 * @param token       Saferpay's handle for this payment page session. Needed after the user returned from
 *                    the payment page, to ask Saferpay for the outcome of the payment (PaymentPage/Assert).
 * @param redirectUrl URL of the Saferpay payment page the browser has to be sent to.
 * @param expiration  point in time until which the token (and thus the payment page session) is valid.
 */
public record SaferpayInitializeResult(String token, String redirectUrl, Instant expiration) {}
