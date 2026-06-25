package ch.admin.bj.swiyu.core.business.modules.trust.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record TrustAdditionalDidsSubmissionResponseDto(
    @NotNull UUID id,
    @NotNull UUID nonce,
    @NotNull String permissionDid,
    @NotNull List<String> didsToAdd,
    @NotNull TrustAdditionalDidsSubmissionStatusDto status
) {}
