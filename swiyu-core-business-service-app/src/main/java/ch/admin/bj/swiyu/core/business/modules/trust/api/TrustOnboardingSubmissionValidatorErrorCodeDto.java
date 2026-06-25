package ch.admin.bj.swiyu.core.business.modules.trust.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingSubmissionValidatorErrorCode", enumAsRef = true)
public enum TrustOnboardingSubmissionValidatorErrorCodeDto {
    EDITING_BLOCKED("editing_blocked");

    private String code;

    TrustOnboardingSubmissionValidatorErrorCodeDto(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }
}
