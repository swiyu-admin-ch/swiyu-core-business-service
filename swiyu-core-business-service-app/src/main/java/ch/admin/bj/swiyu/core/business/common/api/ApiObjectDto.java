package ch.admin.bj.swiyu.core.business.common.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiObject")
public enum ApiObjectDto {
    TRUST_ONBOARDING_SUBMISSION_PARTNER_DOCUMENT("trust_onboarding_submission_partner_document"),
    IDENTIFIER_ENTRY("identifier_entries"),
    STATUSLIST_ENTRY("statuslist_entries");

    private String code;

    ApiObjectDto(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }
}
