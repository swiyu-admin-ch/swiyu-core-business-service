package ch.admin.bj.swiyu.core.business.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the current progress of identity verification for a business partner.
 *
 * <p>State derivation rules (computed on every read from BusinessPartnerIdentity + TrustOnboardingSubmission
 * history — never persisted):
 *
 * <pre>
 * No BPI, no active submission          → VERIFICATION_NOT_STARTED
 * No BPI, latest submission UNSUBMITTED → VERIFICATION_STARTED
 * No BPI, latest submission SUBMITTED   → VERIFICATION_IN_PROGRESS
 * No BPI, latest sub INFORMATION_REQUESTED → VERIFICATION_INFORMATION_REQUESTED
 *
 * BPI ACTIVE, no active submission      → VERIFICATION_SUCCEEDED
 * BPI ACTIVE or DEACTIVATED, latest submission UNSUBMITTED → RE_VERIFICATION_STARTED
 * BPI ACTIVE or DEACTIVATED, latest submission SUBMITTED   → RE_VERIFICATION_IN_PROGRESS
 * BPI ACTIVE or DEACTIVATED, latest sub INFORMATION_REQUESTED → VERIFICATION_INFORMATION_REQUESTED
 *
 * BPI DEACTIVATED, no active submission → RE_VERIFICATION_REQUIRED
 *
 * VERIFICATION_REJECTED / RE_VERIFICATION_REJECTED / RE_VERIFICATION_SUCCEEDED
 *   are reserved for future PROFILE_CHANGE / RENEWAL flows (EID-6620).
 * </pre>
 *
 * <p>Key rule for RE_* states: a partner has a "previous successful onboarding" whenever a
 * {@code BusinessPartnerIdentity} exists (regardless of whether it is ACTIVE or DEACTIVATED).
 * Any new TrustOnboardingSubmission started after that point produces RE_* variants.
 */
@Schema(name = "IdentityVerificationProgress", enumAsRef = true)
public enum IdentityVerificationProgressDto {
    /** No BPI and no active submission — partner has never started verification. */
    VERIFICATION_NOT_STARTED,
    /** No BPI, first submission exists but has not been submitted yet (UNSUBMITTED). */
    VERIFICATION_STARTED,
    /** No BPI, first submission has been submitted and is under review (SUBMITTED). */
    VERIFICATION_IN_PROGRESS,
    /** TMS has requested more information from the partner. */
    VERIFICATION_INFORMATION_REQUESTED,
    /** Reserved for future PROFILE_CHANGE / RENEWAL rejection (EID-6620). */
    VERIFICATION_REJECTED,
    /** BPI is ACTIVE and no new submission is in flight — partner is fully verified. */
    VERIFICATION_SUCCEEDED,
    /** BPI exists but DEACTIVATED, and no new submission has been started yet. */
    RE_VERIFICATION_REQUIRED,
    /** BPI exists, new re-verification submission started but not yet submitted (UNSUBMITTED). */
    RE_VERIFICATION_STARTED,
    /** BPI exists, new re-verification submission submitted and under review (SUBMITTED). */
    RE_VERIFICATION_IN_PROGRESS,
    /** Reserved for future PROFILE_CHANGE / RENEWAL rejection (EID-6620). */
    RE_VERIFICATION_REJECTED,
    /** Reserved for future PROFILE_CHANGE / RENEWAL completion (EID-6620). */
    RE_VERIFICATION_SUCCEEDED,
}
