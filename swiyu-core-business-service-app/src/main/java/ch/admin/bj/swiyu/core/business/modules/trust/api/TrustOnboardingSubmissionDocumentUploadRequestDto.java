package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@Builder
@Validated
@Schema(
    name = "TrustOnboardingSubmissionDocumentUploadRequest",
    description = "A request to upload a document to a trust registry submission"
)
public record TrustOnboardingSubmissionDocumentUploadRequestDto(
    @Valid @NotNull TrustOnboardingSubmissionDocumentTypeDto type,
    @Valid @NotNull @Schema MultipartFile file
) {}
