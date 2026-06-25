package ch.admin.bj.swiyu.core.business.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ApiError")
public record ApiErrorDto(ApiErrorCodeDto errorCode, String message, List<String> additionalDetails) {}
