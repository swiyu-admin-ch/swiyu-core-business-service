package ch.admin.bj.swiyu.core.business.common.email;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes a {@code TiSendEmailCommand} carrying the fully composed email - subject and plain text
 * body in all four languages - for every partner notification triggered by a trust event.
 *
 * <p>One method per email type. Per the feature spec, all event triggered emails need the partner id
 * only; the reviewed texts contain no reject reason or partner note. All methods must be called from
 * within an existing transaction: the command goes through the transactional outbox and must be
 * committed together with the state change that triggered it.
 *
 * <p>The recipient is the designated contact person of the business partner in the service portal.
 * Callers do not resolve it - the implementation looks it up from the partner id. A partner without
 * a contact person is logged and skipped rather than failing the surrounding transaction.
 *
 * <p>This interface deliberately lives in {@code common} while its implementation lives in
 * {@code modules.email}: every module may depend on {@code common}, so the trigger points couple to
 * the abstraction only and no module to module dependency arises.
 */
public interface EmailCommandPublisher {
    void submissionAccepted(UUID partnerId);

    void trustRegistrationSucceeded(UUID partnerId);

    void trustRegistrationRejected(UUID partnerId);

    void trustRegistrationInformationRequested(UUID partnerId);

    void trustProfileChangeSucceeded(UUID partnerId);

    void trustProfileChangeRejected(UUID partnerId);

    void trustProfileChangeInformationRequested(UUID partnerId);

    void trustRenewalSucceeded(UUID partnerId);

    void trustIdentityExpired(UUID partnerId);

    void trustAdditionalDidAdded(UUID partnerId);

    void trustProtectedVerificationAhvApproved(UUID partnerId);

    void trustProtectedVerificationAhvRejected(UUID partnerId);

    void trustReviewDelayed(UUID partnerId, UUID trustOnboardingSubmissionId, int delayDurationDays);

    void trustRenewalReminder(UUID partnerId, Instant validUntil, int daysUntilExpiration);
}
