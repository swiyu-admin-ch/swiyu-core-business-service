package ch.admin.bj.swiyu.core.business.modules.trust.api;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TrustAdditionalDidsSubmissionInternalDto(
    @NotNull UUID id,
    @NotNull TrustAdditionalDidsSubmissionStatusDto status,
    @NotNull ProofOfPossessionDto permissionDid,
    @NotNull List<ProofOfPossessionDto> didsToAdd,
    Instant updatedAt
) {}
