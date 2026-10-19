package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

/**
 * The business purpose of a {@link TrustOnboardingSubmission}: controls how it is created and what the
 * partner may edit. {@link #REGISTRATION} comes from user input; the others are derived from verified
 * partner data and require an already-trusted partner (ACTIVE BusinessPartnerIdentity).
 */
public enum TrustOnboardingSubmissionType {
    /** Initial onboarding of an untrusted partner; all fields user-supplied and editable. */
    REGISTRATION,

    /** Profile update, allowed only <em>within</em> 3 years of trust-identity expiry; UID fixed. */
    PROFILE_CHANGE_MANDATORY,

    /** Profile update, allowed only <em>outside</em> 3 years of expiry; mirror of {@link #PROFILE_CHANGE_MANDATORY}. */
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
