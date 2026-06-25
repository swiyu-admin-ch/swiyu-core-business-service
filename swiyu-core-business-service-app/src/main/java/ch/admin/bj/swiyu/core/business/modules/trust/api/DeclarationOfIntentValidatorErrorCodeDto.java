package ch.admin.bj.swiyu.core.business.modules.trust.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DeclarationOfIntentValidatorErrorCode", enumAsRef = true)
public enum DeclarationOfIntentValidatorErrorCodeDto {
    INVALID_SIGNATURES_FOR_MANDANT("invalid_signatures_for_mandant"),
    NO_SIGNATURES_FOUND("no_signatures_found"),
    VALIDATION_SERVICE_NOT_AVAILABLE("validation_service_not_available"),
    VIOLATING_DOI_VARIANT_SINGLE_SIGNATURE("violating_doi_variant_single_signature"),
    VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_TWO("violating_doi_variant_joint_signature_two"),
    VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_THREE("violating_doi_variant_joint_signature_three");

    private final String code;

    DeclarationOfIntentValidatorErrorCodeDto(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }

    public static DeclarationOfIntentValidatorErrorCodeDto fromCode(String code) {
        for (var value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown DeclarationOfIntentValidatorErrorCode: " + code);
    }
}
