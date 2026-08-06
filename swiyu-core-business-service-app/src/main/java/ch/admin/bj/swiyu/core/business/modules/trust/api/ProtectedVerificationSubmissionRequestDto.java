package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(name = "ProtectedVerificationSubmissionRequest")
public record ProtectedVerificationSubmissionRequestDto(
    @NotNull UUID partnerId,
    @NotNull UUID sbnId,
    @NotBlank String entityName,
    String uid,
    @Valid ContactDto contactPerson,
    @NotNull ProtectedVerificationCategoryDto category,
    @NotBlank @Size(max = 2000) String reason
) {}
