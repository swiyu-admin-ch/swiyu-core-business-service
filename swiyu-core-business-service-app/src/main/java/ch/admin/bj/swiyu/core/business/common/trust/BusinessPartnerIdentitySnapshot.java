package ch.admin.bj.swiyu.core.business.common.trust;

import java.time.Instant;

/**
 * The two facts about a business partner identity that the verification-progress computation needs.
 *
 * <p>Passed in by the caller so that the {@link IdentityVerificationProgressProvider} does not have to
 * read another module's domain: the trust module owns the submission history, the management module
 * owns the identity.
 *
 * @param active     whether the identity is currently {@code ACTIVE}
 * @param validUntil expiry of the identity, may be {@code null}
 */
public record BusinessPartnerIdentitySnapshot(boolean active, Instant validUntil) {}
