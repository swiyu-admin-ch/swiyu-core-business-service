package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(name = "TrustOnboardingSubmissionListItem", enumAsRef = true)
public record TrustOnboardingSubmissionListItemDto(
    @NotNull UUID id,
    @NotNull UUID partnerId,
    @NotNull TrustOnboardingSubmissionStatusDto status,
    @NotNull Instant createdAt,
    @NotNull Instant updatedAt
) {}
