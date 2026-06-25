package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingSubmissionStatus", enumAsRef = true)
public enum TrustOnboardingSubmissionStatusDto {
    UNSUBMITTED, // user can update at any time
    SUBMITTED,
    SUCCEEDED, // approved and statements for all DIDs are published in trust registry
    REJECTED,
    INFORMATION_REQUESTED,
}
