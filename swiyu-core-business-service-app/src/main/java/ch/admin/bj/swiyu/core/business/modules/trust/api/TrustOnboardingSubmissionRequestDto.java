package ch.admin.bj.swiyu.core.business.modules.trust.api;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.i18n.ValidLocalizedMap;
import ch.admin.bj.swiyu.core.business.common.validation.ValidRegistryIds;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Schema(name = "TrustOnboardingSubmissionRequest", description = "A request to onboard a new trust entity.")
public record TrustOnboardingSubmissionRequestDto(
    @Schema(description = "Id of the business partner.", example = "deadbeef-0000-dead-beef-000000000000")
    @Getter
    UUID partnerId,
    @Schema(
        description = "The official name of the entity as localized map.",
        example = "{ \"default\": \"Swiss Confederation\", \"de-CH\": \"Schweizerische Eidgenossenschaft\", \"fr-CH\": \"Confédération suisse\", \"it-CH\": \"Confederazione Svizzera\", \"en-CH\": \"Swiss Confederation\", \"rm-CH\": \"Confederaziun svizra\" }"
    )
    @ValidLocalizedMap
    @Getter
    Map<String, String> entityName,
    @Schema(
        description = "The official address of the entity.",
        example = "{ \"street\": \"Musterstrasse\", \"houseNumber\": \"1\", \"postalCode\": \"3000\", \"city\": \"Bern\", \"country\": \"CH\" }"
    )
    @Valid
    @Getter
    AddressDto entityAddress,
    @Schema(description = "The official email address of the entity.", example = "info@company.com")
    @Getter
    String entityEmail,
    @Schema(
        description = "The contact person for the entity.",
        example = "{ \"firstName\": \"John\", \"lastName\": \"Doe\", \"email\": \"john.doe@company.com\", \"phone\": \"+41 12 345 67 89\" }"
    )
    @Valid
    @Getter
    ContactDto contactPerson,
    @Getter
    @Schema(
        description = "The rule how it can be signed, null for INDIVIDUAL business partner type",
        example = "SINGLE_SIGNATURE"
    )
    SigningRuleDto signingRule,
    @Getter
    @Schema(
        description = "The required signatories, null or empty for INDIVIDUAL business partner type",
        example = "{ \"firstName\": \"John\", \"lastName\": \"Doe\", \"email\": \"john.doe@company.com\", \"phone\": \"+41 12 345 67 89\" }"
    )
    List<@NotNull @Valid SignatoryDto> signatories,
    @Schema(
        description = "A map of registry identifiers for the entity. The key is the registry name (e.g., 'UID') and the value is the identifier.",
        example = "{ \"UID\": \"CHE-123.456.789\" }"
    )
    @ValidRegistryIds
    @Getter
    Map<String, String> registryIds,

    @Schema(description = "Whether or not the entity is registered in the commercial register", example = "false")
    @Getter
    Boolean isRegisteredInCommercialRegister,

    // To be removed with EID-6303
    @Schema(description = "The preferred language for correspondence.", example = "DE")
    @Getter
    LanguageDto correspondingLanguage,

    @Getter @Schema(description = "Selected DIDs to be onboarded initially") List<String> dids,
    @Schema(
        description = "The requested partner type. Can be different than the current partner type of the business partner."
    )
    @Getter
    BusinessPartnerTypeDto requestedPartnerType
) {
    public TrustOnboardingSubmissionRequestDto {
        signatories = (signatories == null) ? emptyList() : signatories;
        registryIds = (registryIds == null) ? emptyMap() : registryIds;
    }
}
