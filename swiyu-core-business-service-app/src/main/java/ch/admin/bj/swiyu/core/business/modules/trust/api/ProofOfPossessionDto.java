package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(name = "ProofOfPossession")
public record ProofOfPossessionDto(
    @NotBlank String did,
    @NotBlank String nonce,
    @NotNull ProofOfPossessionStatusDto status,
    Instant verifiedAt
) {}
