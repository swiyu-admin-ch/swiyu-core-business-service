package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.api.*;
import ch.admin.bj.swiyu.core.business.common.i18n.ValidLocalizedMap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(name = "TrustOnboardingSubmission")
public record TrustOnboardingSubmissionDto(
    @NotNull UUID id,
    @NotNull UUID partnerId,
    @NotNull @ValidLocalizedMap Map<String, String> name,
    @Deprecated(since = "3.38.0", forRemoval = true) // Remove in EID-6303
    @NotNull
    MultiLanguageTextDto entityName,
    @NotNull String entityEmail,
    @NotNull AddressDto address,
    @NotNull ContactDto contactPerson,
    @NotNull Long version,
    @NotNull TrustOnboardingSubmissionStatusDto status,
    @NotNull List<ProofOfPossessionDto> proofOfPossessions,
    @Schema(description = "Type of business partner") BusinessPartnerTypeDto businessPartnerType,
    @Schema(description = "The rule how it can be signed, null for INDIVIDUAL business partner type")
    SigningRuleDto signingRule,
    @Schema(description = "The required signatories, null or empty for INDIVIDUAL business partner type")
    List<@NotNull SignatoryDto> signatories,
    @NotNull Map<String, String> registryIds,
    @Schema(description = "Whether or not the entity is registered in the commercial register")
    Boolean isRegisteredInCommercialRegister,
    String rejectionReason,
    String declineReason,
    String partnerNote,
    LanguageDto correspondingLanguage,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant submittedAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant initiatedAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant createdAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant updatedAt
) implements ListItemDto {}
