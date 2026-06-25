package ch.admin.bj.swiyu.core.business.modules.trust.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TrustAdditionalDidsSubmissionCreateRequestDto(
    @NotBlank String permissionDid,
    @NotNull List<String> didsToAdd
) {}
