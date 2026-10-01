package ch.admin.bj.swiyu.core.business.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for toggling the hard-delete safeguard of a business partner.
 */
@Schema(name = "BusinessPartnerAllowHardDelete")
public record BusinessPartnerAllowHardDeleteDto(
    @Schema(description = "Arms or locks the hard-delete safeguard. Governmental institutions cannot be armed.")
    @NotNull
    Boolean hardDeleteAllowed
) {}
