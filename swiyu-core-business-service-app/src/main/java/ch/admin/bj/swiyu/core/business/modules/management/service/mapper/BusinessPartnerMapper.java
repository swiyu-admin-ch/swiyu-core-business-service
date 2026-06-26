package ch.admin.bj.swiyu.core.business.modules.management.service.mapper;

import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerTypeDto;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ObjectLimitsDto;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper;
import ch.admin.bj.swiyu.core.business.modules.management.api.*;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntityTrustStatus;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

/**
 * Collection of static mapping functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BusinessPartnerMapper {

    public static @NonNull Address toAddress(CreatePartnerDto source) {
        return new Address(
            source.addressStreet(),
            source.addressCity(),
            source.addressZipCode(),
            source.addressCountry(),
            source.addressRegion()
        );
    }

    public static BusinessEntityDto toBusinessEntityDto(BusinessEntity source, List<ObjectLimitsDto> limits) {
        BusinessPartnerTypeDto type = toBusinessPartnerTypeDto(source.getType());
        return new BusinessEntityDto(
            source.getId(),
            LocalizedMapUtil.getDefaultValue(source.getEntityName()),
            source.getContactEmail(),
            type,
            limits,
            source.getTrustVerificationStatus() == BusinessEntityTrustStatus.VERIFIED,
            source.isPayedForTrustVerification(),
            source.getPayedForDidSlots(),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt()
        );
    }

    public static BusinessPartnerDto toBusinessPartnerDto(BusinessEntity source, List<ObjectLimitsDto> limits) {
        BusinessPartnerTypeDto type = toBusinessPartnerTypeDto(source.getType());
        return new BusinessPartnerDto(
            source.getId(),
            LocalizedMapUtil.getDefaultValue(source.getEntityName()),
            source.getEntityName(),
            source.getContactEmail(),
            type,
            limits,
            source.isPayedForTrustVerification(),
            source.getPayedForDidSlots(),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt(),
            source.getUid(),
            AddressMapper.toAddressDto(source.getAddress()),
            source.getContactPhone(),
            toTrustVerificationStatusDto(source.getTrustVerificationStatus()),
            source.getMaxDateForTrustVerificationStatus()
        );
    }

    public static BusinessPartnerListItemDto toBusinessPartnerListItemDto(
        BusinessEntity source,
        List<ObjectLimitsDto> limits
    ) {
        BusinessPartnerTypeDto type = toBusinessPartnerTypeDto(source.getType());
        return new BusinessPartnerListItemDto(
            source.getId(),
            LocalizedMapUtil.getDefaultValue(source.getEntityName()),
            source.getEntityName(),
            type,
            limits,
            source.isPayedForTrustVerification(),
            source.getPayedForDidSlots(),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt(),
            toTrustVerificationStatusDto(source.getTrustVerificationStatus()),
            source.getMaxDateForTrustVerificationStatus()
        );
    }

    private static BusinessPartnerTrustStatusDto toTrustVerificationStatusDto(BusinessEntityTrustStatus source) {
        return switch (source) {
            case NOT_VERIFIED -> BusinessPartnerTrustStatusDto.NOT_VERIFIED;
            case VERIFICATION_STARTED -> BusinessPartnerTrustStatusDto.VERIFICATION_STARTED;
            case VERIFICATION_IN_PROGRESS -> BusinessPartnerTrustStatusDto.VERIFICATION_IN_PROGRESS;
            case INFORMATION_REQUESTED -> BusinessPartnerTrustStatusDto.INFORMATION_REQUESTED;
            case VERIFIED -> BusinessPartnerTrustStatusDto.VERIFIED;
            case RE_VERIFICATION_STARTED -> BusinessPartnerTrustStatusDto.RE_VERIFICATION_STARTED;
            case RE_VERIFICATION_IN_PROGRESS -> BusinessPartnerTrustStatusDto.RE_VERIFICATION_IN_PROGRESS;
        };
    }

    public static BusinessEntityTrustStatus toTrustVerificationStatus(BusinessPartnerTrustStatusDto source) {
        return switch (source) {
            case NOT_VERIFIED -> BusinessEntityTrustStatus.NOT_VERIFIED;
            case VERIFICATION_STARTED -> BusinessEntityTrustStatus.VERIFICATION_STARTED;
            case VERIFICATION_IN_PROGRESS -> BusinessEntityTrustStatus.VERIFICATION_IN_PROGRESS;
            case INFORMATION_REQUESTED -> BusinessEntityTrustStatus.INFORMATION_REQUESTED;
            case VERIFIED -> BusinessEntityTrustStatus.VERIFIED;
            case RE_VERIFICATION_STARTED -> BusinessEntityTrustStatus.RE_VERIFICATION_STARTED;
            case RE_VERIFICATION_IN_PROGRESS -> BusinessEntityTrustStatus.RE_VERIFICATION_IN_PROGRESS;
        };
    }
}
