package ch.admin.bj.swiyu.core.business.common.infrastructure.web.controller;

import ch.admin.bj.swiyu.core.business.common.api.ApiErrorCodeDto;
import ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.validation.FieldError;

/**
 * Collection of static mapping functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RestExceptionMapper {

    public static ApiErrorDto toApiErrorDto(BusinessExceptionErrorCode code, String message) {
        return new ApiErrorDto(RestExceptionMapper.toBusinessExceptionErrorCodeDto(code), message, new ArrayList<>());
    }

    public static ApiErrorDto toApiErrorDto(
        BusinessExceptionErrorCode code,
        String message,
        List<String> additionalDetails
    ) {
        return new ApiErrorDto(RestExceptionMapper.toBusinessExceptionErrorCodeDto(code), message, additionalDetails);
    }

    /**
     * Formats a list of {@link FieldError}s into human-readable strings of the form
     * {@code "fieldName: constraint message"} without exposing the rejected value.
     */
    public static List<String> toFieldErrorDetails(List<FieldError> fieldErrors) {
        return fieldErrors
            .stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
    }

    public static ApiErrorCodeDto toBusinessExceptionErrorCodeDto(BusinessExceptionErrorCode source) {
        return switch (source) {
            case DATA_INVALID -> ApiErrorCodeDto.DATA_INVALID;
            case DATA_INVALID_VIRUS_DETECTED -> ApiErrorCodeDto.DATA_INVALID_VIRUS_DETECTED;
            case RESOURCE_NOT_FOUND -> ApiErrorCodeDto.RESOURCE_NOT_FOUND;
            case RESOURCE_FORBIDDEN -> ApiErrorCodeDto.RESOURCE_FORBIDDEN;
            case ACTION_FORBIDDEN -> ApiErrorCodeDto.ACTION_FORBIDDEN;
            case PARTNER_IS_NOT_GOVERNMENTAL -> ApiErrorCodeDto.PARTNER_IS_NOT_GOVERNMENTAL;
            case BUSINESS_DATA_INTEGRITY_VIOLATION -> ApiErrorCodeDto.BUSINESS_DATA_INTEGRITY_VIOLATION;
            case CRYPTO_INTEGRITY_VALIDATION_FAILED -> ApiErrorCodeDto.CRYPTO_INTEGRITY_VALIDATION_FAILED;
            case DID_RESOLVE_FAILED -> ApiErrorCodeDto.DID_RESOLVE_FAILED;
            case DOCUMENT_NOT_FOUND -> ApiErrorCodeDto.DOCUMENT_NOT_FOUND;
            case IDENTIFIER_VALIDATION_FAILED -> ApiErrorCodeDto.IDENTIFIER_VALIDATION_FAILED;
            case INVALID_PAGINATION -> ApiErrorCodeDto.INVALID_PAGINATION;
            case MAX_SIZE_EXCEEDED -> ApiErrorCodeDto.MAX_SIZE_EXCEEDED;
            case OBJECT_COUNT_LIMIT_REACHED -> ApiErrorCodeDto.OBJECT_COUNT_LIMIT_REACHED;
            case STATUS_LIST_VALIDATION_FAILED -> ApiErrorCodeDto.STATUS_LIST_VALIDATION_FAILED;
            case TRUST_ONBOARDING_DOCUMENT_VALIDATION_FAILED -> ApiErrorCodeDto.TRUST_ONBOARDING_DOCUMENT_VALIDATION_FAILED;
            case VC_SCHEMA_SUBMISSION_NOT_FOUND -> ApiErrorCodeDto.VC_SCHEMA_SUBMISSION_NOT_FOUND;
            case VC_TYPE_METADATA_VALIDATION_FAILED -> ApiErrorCodeDto.VC_TYPE_METADATA_VALIDATION_FAILED;
            case VQPS_PUBLICATION_FAILED -> ApiErrorCodeDto.VQPS_PUBLICATION_FAILED;
            case VQPS_PUBLICATION_WAIT_TIMEOUT -> ApiErrorCodeDto.VQPS_PUBLICATION_WAIT_TIMEOUT;
        };
    }
}
