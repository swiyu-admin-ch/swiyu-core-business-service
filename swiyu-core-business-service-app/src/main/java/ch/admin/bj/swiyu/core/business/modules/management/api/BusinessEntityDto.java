package ch.admin.bj.swiyu.core.business.modules.management.api;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "BusinessEntity")
public record BusinessEntityDto(
    @Schema(description = "Key of the entity under which it is registered with ePortal") @NotBlank UUID id,
    @Schema(description = "Unique name of the entity") @NotBlank String name,
    @NotBlank String contactEmailAddress,
    @Schema(description = "Type of the entity", nullable = true) BusinessPartnerTypeDto type,
    @Schema(description = "Was there a successful trust onboarding") boolean isVerified,
    @Schema(description = "User payed for trust onboarding") boolean payedForTrustVerification,
    @Schema(description = "Number of DID slots the user payed for already") int payedForDIDSlots,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant createdAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant updatedAt
) implements ListItemDto {}
