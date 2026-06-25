package ch.admin.bj.swiyu.core.business.modules.documents.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "PartnerDocument")
public record PartnerDocumentDto(
    @NotNull UUID id,
    @NotNull Instant createdAt,
    @NotNull Instant lastModifiedAt,
    UUID trustOnboardingSubmissionId,
    @NotBlank String fileName,
    @NotBlank String mediaType,
    @NotNull PartnerDocumentTypeDto type,
    @NotNull UUID partnerId,
    @NotNull Instant submittedAt
) {}
