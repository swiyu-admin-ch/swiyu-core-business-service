package ch.admin.bj.swiyu.core.business.common.service.mapper;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BusinessPartnerTypeMapper {

    public static BusinessPartnerType toBusinessPartnerType(BusinessPartnerTypeDto businessPartnerTypeDto) {
        if (businessPartnerTypeDto == null) {
            return BusinessPartnerType.UNKNOWN;
        }
        return switch (businessPartnerTypeDto) {
            case GOVERNMENTAL_INSTITUTION -> BusinessPartnerType.GOVERNMENTAL_INSTITUTION;
            case BUSINESS -> BusinessPartnerType.BUSINESS;
            case INDIVIDUAL -> BusinessPartnerType.INDIVIDUAL;
            case UNKNOWN -> BusinessPartnerType.UNKNOWN;
        };
    }

    public static BusinessPartnerTypeDto toBusinessPartnerTypeDto(BusinessPartnerType businessPartnerType) {
        if (businessPartnerType == null) {
            return BusinessPartnerTypeDto.UNKNOWN;
        }
        return switch (businessPartnerType) {
            case GOVERNMENTAL_INSTITUTION -> BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION;
            case BUSINESS -> BusinessPartnerTypeDto.BUSINESS;
            case INDIVIDUAL -> BusinessPartnerTypeDto.INDIVIDUAL;
            case UNKNOWN -> BusinessPartnerTypeDto.UNKNOWN;
        };
    }
}
