package ch.admin.bj.swiyu.core.business.modules.trust.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingSubmissionDocumentValidatorErrorCode", enumAsRef = true)
public enum TrustOnboardingSubmissionDocumentValidatorErrorCodeDto {
    WRONG_CONTENT_TYPE("wrong_content_type"),
    FILE_SIZE("file_size"),
    STORAGE_CAPACITY_EXCEEDED("storage_capacity_exceeded");

    private String code;

    TrustOnboardingSubmissionDocumentValidatorErrorCodeDto(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }
}
