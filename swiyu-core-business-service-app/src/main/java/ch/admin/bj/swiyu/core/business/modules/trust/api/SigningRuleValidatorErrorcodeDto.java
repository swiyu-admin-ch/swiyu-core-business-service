package ch.admin.bj.swiyu.core.business.modules.trust.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SigningRuleValidatorErrorcode", enumAsRef = true)
public enum SigningRuleValidatorErrorcodeDto {
    SIGNING_RULE_BUSINESSPARTNER_TYPE_MISMATCH("signing_rule_businesspartner_type_mismatch"),
    SIGNING_RULE_SIGNATORY_COUNT_MISMATCH("signing_rule_signatory_count_mismatch");

    private final String code;

    SigningRuleValidatorErrorcodeDto(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }
}
