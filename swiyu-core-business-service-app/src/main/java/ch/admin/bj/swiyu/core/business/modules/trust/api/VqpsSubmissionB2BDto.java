package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Schema(name = "VqpsSubmission")
public record VqpsSubmissionB2BDto(
    @NotNull UUID id,
    @NotNull @Getter UUID partnerId,
    @NotNull Long version,
    @NotNull VqpsSubmissionStatusDto status,
    @Schema(description = "The publication result, only present when in status PUBLICATION_SUCCEEDED")
    VqpsPublicationResultDto publicationResult,
    @Schema(description = "The failure reason, only present when in status PUBLICATION_FAILED")
    VqpsPublicationFailureReasonDto publicationFailureReason,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant createdAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant updatedAt
) implements ListItemDto {
    @JsonIgnore
    public boolean isSucceeded() {
        return VqpsSubmissionStatusDto.PUBLICATION_SUCCEEDED.equals(status);
    }

    @JsonIgnore
    public boolean isFailed() {
        return VqpsSubmissionStatusDto.PUBLICATION_FAILED.equals(status);
    }
}
