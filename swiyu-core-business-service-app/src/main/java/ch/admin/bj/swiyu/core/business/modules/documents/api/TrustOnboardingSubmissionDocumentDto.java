package ch.admin.bj.swiyu.core.business.modules.documents.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(name = "TrustOnboardingSubmissionDocument")
public record TrustOnboardingSubmissionDocumentDto(
    UUID id,
    Instant createdAt,
    Instant updatedAt,

    @Schema(description = "Id of the trust onboarding submission this document belongs to.")
    UUID trustOnboardingSubmissionId,

    @Schema(description = "Download link for the file.", example = "https://example.admin.ch/example.json")
    @NotNull
    URL downloadUrl,

    @Schema(description = "Version number of this document.") @NotNull Long version,

    @Schema(description = "Name of the file.", example = "example.json") @NotBlank String name,

    @Schema(description = "Media type of file.", example = "application/json") @NotNull String mediaType,

    @Schema(description = "Type of file.") @NotNull PartnerDocumentTypeDto type,

    @Schema(description = "Id of the business partner this file belongs to.") @NotNull UUID owningBusinessPartner,

    @Schema(description = "Time and date when this document was submitted.") @NotNull Instant submittedAt
) {}
