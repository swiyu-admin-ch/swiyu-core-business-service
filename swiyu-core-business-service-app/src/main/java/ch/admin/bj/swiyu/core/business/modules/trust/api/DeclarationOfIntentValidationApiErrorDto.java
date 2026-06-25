package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.api.ApiErrorCodeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "DeclarationOfIntentValidationApiError")
public record DeclarationOfIntentValidationApiErrorDto(
    ApiErrorCodeDto errorCode,
    String message,
    List<DeclarationOfIntentValidatorErrorCodeDto> additionalDetails
) {}
