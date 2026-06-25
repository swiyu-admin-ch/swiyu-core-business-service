package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Schema(name = "VcSchemaSubmission")
public record VcSchemaSubmissionDto(
    @Schema @NotBlank UUID id,
    @Schema @NotBlank @Getter UUID partnerId,
    @Schema @NotBlank Long version,
    @Schema @NotBlank VcSchemaSubmissionStatusDto status,
    @Schema @NotBlank String file,
    @Schema String failureReason,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant createdAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant updatedAt
) implements ListItemDto {}
