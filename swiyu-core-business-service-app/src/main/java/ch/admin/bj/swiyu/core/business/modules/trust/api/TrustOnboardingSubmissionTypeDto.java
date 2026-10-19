package ch.admin.bj.swiyu.core.business.modules.trust.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Submission type for trust onboarding")
public enum TrustOnboardingSubmissionTypeDto {
    REGISTRATION,
    PROFILE_CHANGE_MANDATORY,
    PROFILE_CHANGE_VOLUNTARY,
    RENEWAL;

    public boolean isProfileChangeOrRenewal() {
        return (
            this == TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_MANDATORY ||
            this == TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_VOLUNTARY ||
            this == TrustOnboardingSubmissionTypeDto.RENEWAL
        );
    }

    @JsonIgnore
    public boolean isRenewal() {
        return this == TrustOnboardingSubmissionTypeDto.RENEWAL;
    }
}
