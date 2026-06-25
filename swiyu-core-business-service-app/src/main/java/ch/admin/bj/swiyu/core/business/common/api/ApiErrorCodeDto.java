package ch.admin.bj.swiyu.core.business.common.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "ApiErrorCode",
    enumAsRef = true,
    description = """
           | Value                                       | Description                                                                           |
           | ------------------------------------------- | ------------------------------------------------------------------------------------- |
           | data_invalid                                | Indicates that the given data was invalid in syntax or semantic.                      |
           | data_invalid_virus_detected                 | Indicates that the given data contains a virus.                                       |
           | resource_not_found                          | Indicates that the requested data was not found in the given context.                 |
           | resource_forbidden                          | Indicates that the requested data is not available with the permissions used.         |
           | action_forbidden                            | Indicates that the requested action could not be processed with the permissions used. |
           | partner_is_not_governmental                 | Indicates that the partner is not governmental.                                       |
           | business_data_integrity_violation           | Indicates that a business data integrity violation occurred.                          |
           | crypto_integrity_validation_failed          | Indicates that the crypto integrity validation failed.                                |
           | did_resolve_failed                          | Indicates that the DID resolution failed.                                             |
           | document_not_found                          | Indicates that the document was not found.                                            |
           | identifier_validation_failed                | Indicates that the identifier validation failed.                                      |
           | invalid_pagination                          | Indicates that the pagination parameters are invalid.                                 |
           | max_size_exceeded                           | Indicates that the maximum size was exceeded.                                         |
           | object_count_limit_reached                  | Indicates that the object count limit was reached.                                    |
           | status_list_validation_failed               | Indicates that the status list validation failed.                                     |
           | trust_onboarding_document_validation_failed | Indicates that the trust onboarding document validation failed.                       |
           | vc_schema_submission_not_found              | Indicates that the VC schema submission was not found.                                |
           | vc_type_metadata_validation_failed          | Indicates that the VC type metadata validation failed.                                |
    """
)
public enum ApiErrorCodeDto {
    DATA_INVALID("data_invalid"),
    DATA_INVALID_VIRUS_DETECTED("data_invalid_virus_detected"),
    RESOURCE_NOT_FOUND("resource_not_found"),
    RESOURCE_FORBIDDEN("resource_forbidden"),
    ACTION_FORBIDDEN("action_forbidden"),
    PARTNER_IS_NOT_GOVERNMENTAL("partner_is_not_governmental"),
    BUSINESS_DATA_INTEGRITY_VIOLATION("business_data_integrity_violation"),
    CRYPTO_INTEGRITY_VALIDATION_FAILED("crypto_integrity_validation_failed"),
    DID_RESOLVE_FAILED("did_resolve_failed"),
    DOCUMENT_NOT_FOUND("document_not_found"),
    IDENTIFIER_VALIDATION_FAILED("identifier_validation_failed"),
    INVALID_PAGINATION("invalid_pagination"),
    MAX_SIZE_EXCEEDED("max_size_exceeded"),
    OBJECT_COUNT_LIMIT_REACHED("object_count_limit_reached"),
    STATUS_LIST_VALIDATION_FAILED("status_list_validation_failed"),
    TRUST_ONBOARDING_DOCUMENT_VALIDATION_FAILED("trust_onboarding_document_validation_failed"),
    VC_SCHEMA_SUBMISSION_NOT_FOUND("vc_schema_submission_not_found"),
    VC_TYPE_METADATA_VALIDATION_FAILED("vc_type_metadata_validation_failed"),
    VQPS_PUBLICATION_FAILED("vqps_publication_failed"),
    VQPS_PUBLICATION_WAIT_TIMEOUT("vqps_publication_wait_timeout");

    // Keep lowercase values for backwards compatibility.
    private final String code;

    ApiErrorCodeDto(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }
}
