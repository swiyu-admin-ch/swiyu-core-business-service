package ch.admin.bj.swiyu.core.business.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "IdentifierUpdateRequest")
public record IdentifierUpdateRequestDto(
    @Size(max = 255, message = "Description cannot exceed 255 characters") String description
) {}
