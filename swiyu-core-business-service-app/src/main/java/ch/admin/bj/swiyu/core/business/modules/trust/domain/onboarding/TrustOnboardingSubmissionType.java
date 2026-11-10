package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

/**
 * The business purpose of a {@link TrustOnboardingSubmission}: controls how it is created and what the
 * partner may edit. {@link #REGISTRATION} comes from user input; the others are derived from verified
 * partner data and require an already-trusted partner (ACTIVE BusinessPartnerIdentity).
 */
public enum TrustOnboardingSubmissionType {
    /**
     * Initial onboarding of an untrusted partner; all fields user-supplied and editable.
     */
    REGISTRATION,

    /**
     * Mandatory Profile update, allowed only <em>within</em> 3 years of trust-identity expiry; UID fixed. When the
     * submission gets rejected, the partner loses the trust status at the <em>original</em> expiry date rather than on
     * the day of the decision: the TMS leaves the BusinessPartnerIdentity untouched and its scheduled job deactivates
     * it once validUntil has passed (OB_FR_12.8, MUT_23).
     */
    PROFILE_CHANGE_MANDATORY,

    /**
     * Voluntary Profile update, allowed at any time. When the submission gets rejected, all trust statements remain
     * with their original expiration date since BusinessPartnerIdentity in TMS stays untouched.
     */
    PROFILE_CHANGE_VOLUNTARY,

    /**
     * Renewal of an expiring trust identity. Most restrictive: all fields fixed, every trusted DID must be
     * re-validated, DOI/documents copied from the latest submission and non-deletable.
     */
    RENEWAL;

    public boolean isRenewal() {
        return this == RENEWAL;
    }

    /**
     * True for the three re-verification types, which are derived from verified partner data instead of user input.
     * They share the restrictions that follow from that - above all that the DID selection is fixed to the already
     * trusted identifiers.
     */
    public boolean isProfileChangeOrRenewal() {
        return this == PROFILE_CHANGE_MANDATORY || this == PROFILE_CHANGE_VOLUNTARY || this == RENEWAL;
    }
}
