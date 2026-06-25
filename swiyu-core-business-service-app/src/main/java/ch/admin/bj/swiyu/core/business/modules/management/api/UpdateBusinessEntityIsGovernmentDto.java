package ch.admin.bj.swiyu.core.business.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateBusinessEntityIsGovernment")
public record UpdateBusinessEntityIsGovernmentDto(
    @NotNull
    @Schema(
        description = "Only temporary for TEST. Allow set the the businessPartner as government. Must be removed before end of PI16",
        example = "true"
    )
    Boolean isGovernment
) {}
