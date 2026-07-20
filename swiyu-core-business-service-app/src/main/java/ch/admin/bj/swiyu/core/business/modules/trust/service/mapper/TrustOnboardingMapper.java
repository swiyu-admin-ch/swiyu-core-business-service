package ch.admin.bj.swiyu.core.business.modules.trust.service.mapper;

import static ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil.getByLanguageOrDefault;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerTypeDto;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.api.MultiLanguageTextDto;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TrustOnboardingMapper {

    public static @NotNull PartnerDocumentTypeDto toPartnerDocumentTypeDto(
        @Valid @NotNull TrustOnboardingSubmissionDocumentTypeDto source
    ) {
        return switch (source) {
            case TRUST_ONBOARDING_OTHER -> PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER;
            case TRUST_ONBOARDING_DECLARATION_OF_INTENT -> PartnerDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT;
        };
    }

    public static TrustOnboardingSubmissionDto toTrustOnboardingSubmissionDto(TrustOnboardingSubmission source) {
        return new TrustOnboardingSubmissionDto(
            source.getId(),
            source.getPartnerId(),
            source.getEntityName(),
            toMultiLanguageDto(source.getEntityName()),
            source.getEntityEmail(),
            AddressMapper.toAddressDto(source.getEntityAddress()),
            toContactDto(source.getContactPerson()),
            source.getVersion(),
            toTrustOnboardingSubmissionStatusDto(source.getStatus()),
            toProofOfPossessionDto(source.getProofOfPossessions()),
            toBusinessPartnerTypeDto(source.getRequestedPartnerType()),
            toSigningRuleDto(source.getSigningRule()),
            toSignatoryDtos(source.getSignatories()),
            source.getUid() == null ? emptyMap() : Collections.singletonMap("UID", source.getUid()), // EID-5476
            source.getIsRegisteredInCommercialRegister(),
            source.getRejectReason() == null ? null : String.valueOf(source.getRejectReason()),
            source.getDeclineReason() == null ? null : String.valueOf(source.getDeclineReason()),
            source.getPartnerNote(),
            toLanguageDto(source.getCorrespondingLanguage()),
            truncateInstantToMicroseconds(source.getSubmittedAt()),
            truncateInstantToMicroseconds(source.getInitiatedAt()),
            truncateInstantToMicroseconds(source.getAuditMetadata().getCreatedAt()),
            truncateInstantToMicroseconds(source.getAuditMetadata().getLastModifiedAt())
        );
    }

    private static List<ProofOfPossessionDto> toProofOfPossessionDto(List<ProofOfPossession> source) {
        if (isEmpty(source)) {
            return emptyList();
        }
        return source.stream().map(TrustOnboardingMapper::toProofOfPossessionDto).toList();
    }

    private static ProofOfPossessionDto toProofOfPossessionDto(ProofOfPossession source) {
        return new ProofOfPossessionDto(
            source.getDid(),
            source.getNonce(),
            toProofOfPossessionStatusDto(source.getStatus()),
            truncateInstantToMicroseconds(source.getVerifiedAt())
        );
    }

    private static ProofOfPossessionStatusDto toProofOfPossessionStatusDto(ProofOfPossessionStatus source) {
        if (source == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        return switch (source) {
            case VALID -> ProofOfPossessionStatusDto.VALID;
            case NOT_SUPPLIED -> ProofOfPossessionStatusDto.NOT_SUPPLIED;
        };
    }

    // Language
    public static LanguageDto toLanguageDto(Language language) {
        if (language == null) return null;
        return switch (language) {
            case EN -> LanguageDto.EN;
            case FR -> LanguageDto.FR;
            case IT -> LanguageDto.IT;
            case RM -> LanguageDto.RM;
            case DE -> LanguageDto.DE;
        };
    }

    public static Language toLanguageEntity(LanguageDto dto) {
        if (dto == null) {
            return null;
        }
        return switch (dto) {
            case EN -> Language.EN;
            case FR -> Language.FR;
            case IT -> Language.IT;
            case RM -> Language.RM;
            case DE -> Language.DE;
        };
    }

    // Contact
    public static ContactDto toContactDto(Contact contact) {
        if (contact == null) {
            return null;
        }

        Address address = contact.getAddress();
        AddressDto addressDto =
            address == null
                ? null
                : new AddressDto(
                      address.getStreet(),
                      address.getCity(),
                      address.getPostalCode(),
                      address.getCountry(),
                      address.getRegion()
                  );

        return new ContactDto(
            contact.getFirstName(),
            contact.getLastName(),
            contact.getEmail(),
            contact.getPhone(),
            addressDto
        );
    }

    public static Contact toContactEntity(ContactDto dto) {
        if (dto == null) {
            return null;
        }

        AddressDto addressDto = dto.address();
        Address address =
            addressDto == null
                ? null
                : Address.builder()
                      .street(addressDto.street())
                      .city(addressDto.city())
                      .postalCode(addressDto.postalCode())
                      .country(addressDto.country())
                      .build();

        return Contact.builder()
            .firstName(dto.firstName())
            .lastName(dto.lastName())
            .email(dto.email())
            .phone(dto.phone())
            .address(address)
            .build();
    }

    public static List<ProofOfPossession> toProofOfPossession(List<String> dids) {
        if (dids == null || dids.isEmpty()) {
            return emptyList();
        }
        return dids
            .stream()
            .map(did -> new ProofOfPossession(did, UUID.randomUUID().toString()))
            .toList();
    }

    public static TrustOnboardingRejectReason toTrustOnboardingRejectReason(String source) {
        if (source == null) {
            return null;
        }
        var reason = switch (source) {
            case "INCOMPLETE_INFORMATION" -> TrustOnboardingRejectReason.INCOMPLETE_INFORMATION;
            case "INACCURATE_INFORMATION" -> TrustOnboardingRejectReason.INACCURATE_INFORMATION;
            case "OUTDATED_INFORMATION" -> TrustOnboardingRejectReason.OUTDATED_INFORMATION;
            case "IDENTITY_VERIFICATION_FAILURE" -> TrustOnboardingRejectReason.IDENTITY_VERIFICATION_FAILURE;
            case "LACK_OF_AUTHORIZATION" -> TrustOnboardingRejectReason.LACK_OF_AUTHORIZATION;
            case "TECHNICAL_ISSUES" -> TrustOnboardingRejectReason.TECHNICAL_ISSUES;
            case "DUPLICATE_APPLICATION" -> TrustOnboardingRejectReason.DUPLICATE_APPLICATION;
            case "NO_RESPONSE_FROM_APPLICANT" -> TrustOnboardingRejectReason.NO_RESPONSE_FROM_APPLICANT;
            case "FRAUDULENT_ACTIVITY" -> TrustOnboardingRejectReason.FRAUDULENT_ACTIVITY;
            case "OTHER" -> TrustOnboardingRejectReason.OTHER;
            default -> null;
        };
        if (reason == null) {
            log.error("Unknown reject reason: {}. Mapping it to OTHER.", source);
            reason = TrustOnboardingRejectReason.OTHER;
        }
        return reason;
    }

    public static TrustOnboardingDeclineReason toTrustOnboardingDeclineReason(String source) {
        if (source == null) {
            return null;
        }
        var reason = switch (source) {
            case "MISSING_DOCUMENTS" -> TrustOnboardingDeclineReason.MISSING_DOCUMENTS;
            case "UNAUTHORIZED_SIGNATORIES" -> TrustOnboardingDeclineReason.UNAUTHORIZED_SIGNATORIES;
            case "INCORRECT_COMPANY_INFORMATION" -> TrustOnboardingDeclineReason.INCORRECT_COMPANY_INFORMATION;
            case "INCORRECT_DECLARATION_OF_INTENT" -> TrustOnboardingDeclineReason.INCORRECT_DECLARATION_OF_INTENT;
            case "OTHER" -> TrustOnboardingDeclineReason.OTHER;
            default -> null;
        };
        if (reason == null) {
            log.error("Unknown decline reason: {}. Mapping it to OTHER.", source);
            reason = TrustOnboardingDeclineReason.OTHER;
        }
        return reason;
    }

    // Status
    public static TrustOnboardingSubmissionStatusDto toTrustOnboardingSubmissionStatusDto(
        TrustOnboardingSubmissionStatus status
    ) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        return switch (status) {
            case UNSUBMITTED, UNSUBMITTED_TIMEOUT -> TrustOnboardingSubmissionStatusDto.UNSUBMITTED;
            case SUBMITTED -> TrustOnboardingSubmissionStatusDto.SUBMITTED;
            case SUCCEEDED -> TrustOnboardingSubmissionStatusDto.SUCCEEDED;
            case REJECTED -> TrustOnboardingSubmissionStatusDto.REJECTED;
            case INFORMATION_REQUESTED -> TrustOnboardingSubmissionStatusDto.INFORMATION_REQUESTED;
        };
    }

    public static TrustOnboardingSubmissionListItemDto toTrustOnboardingSubmissionListItemDto(
        TrustOnboardingSubmission source
    ) {
        return TrustOnboardingSubmissionListItemDto.builder()
            .id(source.getId())
            .partnerId(source.getPartnerId())
            .status(toTrustOnboardingSubmissionStatusDto(source.getStatus()))
            .createdAt(source.getAuditMetadata().getCreatedAt())
            .updatedAt(source.getAuditMetadata().getLastModifiedAt())
            .build();
    }

    public static SigningRule toSigningRule(SigningRuleDto dto) {
        if (dto == null) {
            return null;
        }
        return switch (dto) {
            case SINGLE_SIGNATURE -> SigningRule.SINGLE_SIGNATURE;
            case JOINT_SIGNATURE_TWO -> SigningRule.JOINT_SIGNATURE_TWO;
            case JOINT_SIGNATURE_THREE -> SigningRule.JOINT_SIGNATURE_THREE;
        };
    }

    public static SigningRuleDto toSigningRuleDto(SigningRule entity) {
        if (entity == null) {
            return null;
        }
        return switch (entity) {
            case SINGLE_SIGNATURE -> SigningRuleDto.SINGLE_SIGNATURE;
            case JOINT_SIGNATURE_TWO -> SigningRuleDto.JOINT_SIGNATURE_TWO;
            case JOINT_SIGNATURE_THREE -> SigningRuleDto.JOINT_SIGNATURE_THREE;
        };
    }

    public static List<Signatory> toSignatories(List<SignatoryDto> dtos) {
        if (isEmpty(dtos)) {
            return emptyList();
        }
        return dtos
            .stream()
            .map(dto -> new Signatory(dto.firstName(), dto.lastName(), dto.phone(), dto.email()))
            .toList();
    }

    public static List<SignatoryDto> toSignatoryDtos(List<Signatory> entities) {
        if (isEmpty(entities)) {
            return emptyList();
        }
        return entities
            .stream()
            .map(entity -> new SignatoryDto(entity.firstName(), entity.lastName(), entity.phone(), entity.email()))
            .toList();
    }

    private static Instant truncateInstantToMicroseconds(Instant source) {
        if (source == null) {
            return null;
        }
        return source.truncatedTo(ChronoUnit.MICROS);
    }

    private static MultiLanguageTextDto toMultiLanguageDto(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        return new MultiLanguageTextDto(
            getByLanguageOrDefault(map, Language.DE),
            getByLanguageOrDefault(map, Language.FR),
            getByLanguageOrDefault(map, Language.IT),
            getByLanguageOrDefault(map, Language.EN),
            getByLanguageOrDefault(map, Language.RM)
        );
    }
}
