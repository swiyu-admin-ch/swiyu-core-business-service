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
     * submission gets rejected, all trust statements get deactivated since BusinessPartnerIdentity in TMS is getting
     * deactivated.
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
}
