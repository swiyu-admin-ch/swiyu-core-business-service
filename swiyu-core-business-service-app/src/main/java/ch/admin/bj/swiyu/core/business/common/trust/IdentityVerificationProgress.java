package ch.admin.bj.swiyu.core.business.common.trust;

import java.time.Instant;

/**
 * Current progress of the identity verification of a business partner, together with an optional
 * deadline by which the current verification step must be completed.
 *
 * @param status           the computed progress state
 * @param maxDateForStatus the deadline of the current step, or {@code null} if none applies
 */
public record IdentityVerificationProgress(IdentityVerificationProgressStatusDto status, Instant maxDateForStatus) {
    /** Convenience factory — use when no deadline applies. */
    public static IdentityVerificationProgress of(IdentityVerificationProgressStatusDto status) {
        return new IdentityVerificationProgress(status, null);
    }
}
