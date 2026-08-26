package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Submission type for trust onboarding")
public enum TrustOnboardingSubmissionTypeDto {
    REGISTRATION,
    PROFILE_CHANGE,
    RENEWAL,
}
