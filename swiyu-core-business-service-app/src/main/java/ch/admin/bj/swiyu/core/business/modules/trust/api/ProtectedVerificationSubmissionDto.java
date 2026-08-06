package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(name = "ProtectedVerificationSubmission")
public record ProtectedVerificationSubmissionDto(
    @NotNull UUID id,
    @NotNull UUID partnerId,
    @NotNull UUID sbnId, // unique identifier for organizations authorized by ZAS
    @NotNull String entityName,
    String uid,
    ContactDto contactPerson,
    @NotNull ProtectedVerificationCategoryDto category,
    @NotNull String reason,
    @NotNull ProtectedVerificationSubmissionStatusDto status,
    @Schema(example = "2024-10-29T09:35:16.809924Z") @NotNull Instant submittedAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") @NotNull Instant createdAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") @NotNull Instant updatedAt
) implements ListItemDto {}
