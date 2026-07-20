package ch.admin.bj.swiyu.core.business.modules.management.api;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.i18n.ValidLocalizedMap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(name = "BusinessPartner")
public record BusinessPartnerDto(
    @Schema(description = "Key of the partner under which it is registered with ePortal") @NotBlank UUID id,
    // To be removed in EID-6303
    @Schema(description = "Unique name of the partner. Deprecated: use entityName.", deprecated = true)
    @NotBlank
    String name,
    @Schema(description = "Localized entity name map with required default key and BCP-47 locale keys")
    @NotNull
    @ValidLocalizedMap
    Map<String, String> entityName,
    @NotBlank String contactEmailAddress,
    @Schema(description = "Type of the partner") @NotNull BusinessPartnerTypeDto type,
    @Schema(description = "User paid for trust onboarding") boolean payedForTrustVerification,
    @Schema(description = "Number of DID slots the user paid for already") int payedForDIDSlots,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant createdAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant updatedAt,
    @Schema(description = "Enterprise identification number of the partner") String uid,
    @Schema(description = "Address of the partner") AddressDto address,
    @NotBlank String contactPhone,
    @Schema(description = "Aggregated state of the trust process for this business partner")
    BusinessPartnerTrustStatusDto trustVerificationStatus,
    @Schema(description = "Time limit, if necessary, of the current aggregated state of the trust process")
    Instant maxDateForTrustVerificationStatus
) {}
