package ch.admin.bj.swiyu.core.business.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration of all possible identity-verification progress states for a business partner.
 *
 * <p>State derivation rules (computed on every read from BusinessPartnerIdentity +
 * TrustOnboardingSubmission history — never persisted):
 *
 * <pre>
 * No BPI, no active submission             → VERIFICATION_NOT_STARTED
 * No BPI, latest submission UNSUBMITTED    → VERIFICATION_STARTED
 * No BPI, latest submission SUBMITTED      → VERIFICATION_IN_PROGRESS
 * No BPI, latest sub INFORMATION_REQUESTED/RESUBMITTED
 *                                          → VERIFICATION_INFORMATION_REQUESTED_REQUIRED
 * No BPI, latest sub UNSUBMITTED after resubmit (resubmitRequiredUntil set)
 *                                          → VERIFICATION_INFORMATION_REQUESTED_STARTED
 *
 * BPI ACTIVE, no active submission         → VERIFICATION_SUCCEEDED
 * BPI ACTIVE or DEACTIVATED, latest submission UNSUBMITTED    → RE_VERIFICATION_STARTED
 * BPI ACTIVE or DEACTIVATED, latest submission SUBMITTED      → RE_VERIFICATION_IN_PROGRESS
 * BPI ACTIVE or DEACTIVATED, latest sub INFORMATION_REQUESTED/RESUBMITTED
 *                                          → VERIFICATION_INFORMATION_REQUESTED_REQUIRED
 *
 * BPI ACTIVATED and validUntil less than 90 days from now    → RE_VERIFICATION_REQUIRED
 *
 * VERIFICATION_REJECTED / RE_VERIFICATION_REJECTED / RE_VERIFICATION_SUCCEEDED
 *   are reserved for future PROFILE_CHANGE / RENEWAL flows (EID-6620).
 * </pre>
 *
 * <p>Key rule for RE_* states: a partner has a "previous successful onboarding" whenever a
 * {@code BusinessPartnerIdentity} exists (regardless of whether it is ACTIVE or DEACTIVATED).
 * Any new TrustOnboardingSubmission started after that point produces RE_* variants.
 */
@Schema(name = "IdentityVerificationProgressStatus", enumAsRef = true)
public enum IdentityVerificationProgressStatusDto {
    /// No BPI and no active submission — partner has never started verification.
    VERIFICATION_NOT_STARTED,

    /// No BPI, latest submission exists but has not been submitted yet (UNSUBMITTED).
    VERIFICATION_STARTED,

    /// No BPI, latest submission has been submitted and is under review (SUBMITTED/RESUBMITTED).
    VERIFICATION_IN_PROGRESS,

    /// TMS requested more information and the partner has not started adjusting yet (INFORMATION_REQUESTED).
    VERIFICATION_INFORMATION_REQUESTED_REQUIRED,

    /// TMS requested more information and the partner has started adjusting (submission back in UNSUBMITTED but submittedAt is set).
    VERIFICATION_INFORMATION_REQUESTED_STARTED,

    /// Allow the user to know why his submission is rejected
    /// - no BPI (never be trusted) and last submission is rejected
    /// - BPI is deactivated and last submission is rejected and of type PROFILE_CHANGE_MANDATORY
    VERIFICATION_REJECTED,

    /// BPI is ACTIVE and last submission SUCCEEDED.
    VERIFICATION_SUCCEEDED,

    /// BPI activated, and valid until is below 90 days (config)
    RE_VERIFICATION_REQUIRED,

    /// BPI exists (activated or deactivated) and last submission of type PROFILE_CHANGE_MANDATORY, PROFILE_CHANGE_VOLUNTARY or RENEWAL has status UNSUBMITTED.
    RE_VERIFICATION_STARTED,

    /// BPI exists (activated or deactivated) and last submission of type PROFILE_CHANGE_MANDATORY, PROFILE_CHANGE_VOLUNTARY or RENEWAL has status SUBMITTED.
    RE_VERIFICATION_IN_PROGRESS,

    /// Allow the user to know why he is rejected
    /// - BPI is activated and last submission of type PROFILE_CHANGE_VOLUNTARY rejected. Once acknowledged -> VERIFICATION_SUCCEEDED
    /// - BPI is deactivated and last submission of type PROFILE_CHANGE_MANDATORY. Once acknowledged -> VERIFICATION_NOT_STARTED
    RE_VERIFICATION_REJECTED,

    /// BPI is activated and last submission is SUCCEEDED and of type RENEWAL, PROFILE_CHANGE_VOLUNTARY or PROFILE_CHANGE_MANDATORY
    RE_VERIFICATION_SUCCEEDED,
}
