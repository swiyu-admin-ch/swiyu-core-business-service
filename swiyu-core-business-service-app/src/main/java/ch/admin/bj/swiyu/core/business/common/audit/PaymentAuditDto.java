package ch.admin.bj.swiyu.core.business.common.audit;

import java.util.UUID;

/**
 * DTO for payment audit events.
 * Used by {@link AuditPublisher#paymentSucceeded(PaymentAuditDto)} to avoid
 * dependency on the payment domain entity from the common module.
 */
public record PaymentAuditDto(UUID id, Long version, UUID businessPartnerId, String paymentJson) {}
