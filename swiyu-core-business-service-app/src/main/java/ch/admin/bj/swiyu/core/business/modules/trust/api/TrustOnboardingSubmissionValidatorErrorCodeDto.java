package ch.admin.bj.swiyu.core.business.modules.trust.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingSubmissionValidatorErrorCode", enumAsRef = true)
public enum TrustOnboardingSubmissionValidatorErrorCodeDto {
    EDITING_BLOCKED("editing_blocked"),
    SUBMISSION_DEADLINE_EXPIRED("submission_deadline_expired");

    private String code;

    @JsonCreator
    TrustOnboardingSubmissionValidatorErrorCodeDto(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }
}
