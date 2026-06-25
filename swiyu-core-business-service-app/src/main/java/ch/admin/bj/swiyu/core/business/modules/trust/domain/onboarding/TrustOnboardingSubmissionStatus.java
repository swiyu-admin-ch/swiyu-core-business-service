package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

public enum TrustOnboardingSubmissionStatus {
    UNSUBMITTED, // user can update at any time
    UNSUBMITTED_TIMEOUT,
    SUBMITTED,
    SUCCEEDED, // approved and statements for all DIDs are published in trust registry
    REJECTED,
    INFORMATION_REQUESTED,
}
