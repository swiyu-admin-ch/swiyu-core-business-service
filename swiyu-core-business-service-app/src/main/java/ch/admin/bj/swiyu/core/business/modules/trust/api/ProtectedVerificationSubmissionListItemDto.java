package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(name = "ProtectedVerificationSubmissionListItem")
public record ProtectedVerificationSubmissionListItemDto(
    @NotNull UUID id,
    @NotNull UUID partnerId,
    @NotNull String entityName,
    @NotNull ProtectedVerificationCategoryDto category,
    @NotNull ProtectedVerificationSubmissionStatusDto status,
    @Schema(example = "2024-10-29T09:35:16.809924Z") @NotNull Instant submittedAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") @NotNull Instant createdAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") @NotNull Instant updatedAt
) {}
