package ch.admin.bj.swiyu.core.business.common.trust;

import jakarta.annotation.Nullable;
import java.util.UUID;

/**
 * Computes the identity verification progress of a business partner from its
 * {@code BusinessPartnerIdentity} and its trust onboarding submission history.
 *
 * <p>The progress is derived on every read, never persisted. The caller keeps deciding the
 * "no active DID document yet" rule: verification cannot be started without a DID, so a computed
 * {@code VERIFICATION_STARTED} is reported as {@code VERIFICATION_NOT_STARTED} when the partner has
 * no active identifier entry.
 *
 * <p>Must be called from within an existing read transaction.
 *
 * <p>This interface deliberately lives in {@code common} while its implementation lives in
 * {@code modules.trust.service}: every module may depend on {@code common}, so callers couple to the
 * abstraction only and no module to module dependency arises.
 */
public interface IdentityVerificationProgressProvider {
    /**
     * @param businessPartnerId the partner to compute the progress for
     * @param identity          the partner's identity facts, or {@code null} if the partner has no
     *                          identity yet
     * @return the current progress, including the deadline of the running step when one applies
     */
    IdentityVerificationProgress verificationProgress(
        UUID businessPartnerId,
        @Nullable BusinessPartnerIdentitySnapshot identity
    );
}
