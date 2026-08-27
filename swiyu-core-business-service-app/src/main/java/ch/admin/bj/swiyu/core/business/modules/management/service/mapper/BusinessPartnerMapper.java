package ch.admin.bj.swiyu.core.business.modules.management.service.mapper;

import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerTypeDto;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper;
import ch.admin.bj.swiyu.core.business.modules.management.api.*;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Collection of static mapping functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BusinessPartnerMapper {

    public static Address toAddress(AddressDto source) {
        return AddressMapper.toAddressEntity(source);
    }

    public static Contact toContact(ContactDto source) {
        if (source == null) {
            return null;
        }
        return Contact.builder()
            .firstName(source.firstName())
            .lastName(source.lastName())
            .email(source.email())
            .phone(source.phone())
            .correspondingLanguage(toLanguage(source.correspondingLanguage()))
            .build();
        // note: deprecated source.address() is intentionally not mapped
    }

    @SuppressWarnings("java:S1874") // Remove with EID-6624
    public static BusinessEntityDto toBusinessEntityDto(BusinessEntity source) {
        BusinessPartnerTypeDto type = toBusinessPartnerTypeDto(source.getType());
        return new BusinessEntityDto(
            source.getId(),
            LocalizedMapUtil.getDefaultValue(source.getEntityName()),
            source.getContactEmail(),
            type,
            source.isBusinessPartnerIdentityActive(),
            source.isPayedForTrustVerification(),
            source.getPayedForDidSlots(),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt()
        );
    }

    /**
     * Maps a BusinessEntity to a BusinessPartnerDto.
     *
     * @param source      the entity to map
     * @param trustStatus the computed trust verification status — must be passed in by the caller
     *                    (computed on-the-fly by BusinessPartnerService, never persisted)
     * @param maxDateForTrustVerificationStatus the computed deadline for the current trust verification
     *                    state, or {@code null} if not applicable
     */
    @SuppressWarnings("java:S1874") // Remove with EID-6624
    public static BusinessPartnerDto toBusinessPartnerDto(
        BusinessEntity source,
        BusinessPartnerTrustStatusDto trustStatus,
        Instant maxDateForTrustVerificationStatus
    ) {
        BusinessPartnerTypeDto type = toBusinessPartnerTypeDto(source.getType());
        return new BusinessPartnerDto(
            source.getId(),
            // deprecated: derive from entityName default
            LocalizedMapUtil.getDefaultValue(source.getEntityName()),
            source.getEntityName(),
            // deprecated: derive from contact.email
            source.getContactEmail(),
            type,
            source.isPayedForTrustVerification(),
            source.getPayedForDidSlots(),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt(),
            source.getUid(),
            AddressMapper.toAddressDto(source.getAddress()),
            // deprecated: derive from contact.phone
            source.getContactPhone(),
            // deprecated: computed on-the-fly from BPI + submission history (never persisted)
            trustStatus,
            maxDateForTrustVerificationStatus,
            toContactDto(source.getContact()),
            toBusinessPartnerIdentityDto(source.getBusinessPartnerIdentity())
        );
    }

    /**
     * Maps a BusinessEntity to a BusinessPartnerListItemDto.
     *
     * @param source      the entity to map
     * @param trustStatus the computed trust verification status — must be passed in by the caller
     *                    (computed on-the-fly by BusinessPartnerService, never persisted)
     * @param maxDateForTrustVerificationStatus the computed deadline for the current trust verification
     *                    state, or {@code null} if not applicable
     */
    @SuppressWarnings("java:S1874") // Remove with EID-6624
    public static BusinessPartnerListItemDto toBusinessPartnerListItemDto(
        BusinessEntity source,
        BusinessPartnerTrustStatusDto trustStatus,
        Instant maxDateForTrustVerificationStatus
    ) {
        BusinessPartnerTypeDto type = toBusinessPartnerTypeDto(source.getType());
        return new BusinessPartnerListItemDto(
            source.getId(),
            LocalizedMapUtil.getDefaultValue(source.getEntityName()),
            source.getEntityName(),
            type,
            source.isPayedForTrustVerification(),
            source.getPayedForDidSlots(),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt(),
            trustStatus,
            maxDateForTrustVerificationStatus
        );
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6624
    private static ContactDto toContactDto(Contact source) {
        if (source == null) {
            return null;
        }
        return ContactDto.builder()
            .firstName(source.getFirstName())
            .lastName(source.getLastName())
            .email(source.getEmail())
            .phone(source.getPhone())
            .correspondingLanguage(toLanguageDto(source.getCorrespondingLanguage()))
            .address(null) // deprecated field — not stored on BusinessEntity
            .build();
    }

    private static BusinessPartnerIdentityDto toBusinessPartnerIdentityDto(BusinessPartnerIdentity source) {
        if (source == null) {
            return null;
        }
        return new BusinessPartnerIdentityDto(
            source.getValidUntil(),
            source.getTrustedIdentifier(),
            toBusinessPartnerIdentityStatusDto(source.getStatus()),
            source.getLastActivated(),
            source.getUid(),
            source.getEntityName()
        );
    }

    private static BusinessPartnerIdentityStatusDto toBusinessPartnerIdentityStatusDto(
        BusinessPartnerIdentityStatus source
    ) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case ACTIVE -> BusinessPartnerIdentityStatusDto.ACTIVE;
            case DEACTIVATED -> BusinessPartnerIdentityStatusDto.DEACTIVATED;
        };
    }

    private static LanguageDto toLanguageDto(Language source) {
        if (source == null) {
            return null;
        }
        return LanguageDto.valueOf(source.name());
    }

    private static Language toLanguage(LanguageDto source) {
        if (source == null) {
            return null;
        }
        return Language.valueOf(source.name());
    }
}
