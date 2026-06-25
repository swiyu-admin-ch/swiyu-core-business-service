package ch.admin.bj.swiyu.core.business.common.service.mapper;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressMapper {

    // Address
    public static AddressDto toAddressDto(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDto(
            address.getStreet(),
            address.getCity(),
            address.getPostalCode(),
            address.getCountry(),
            address.getRegion()
        );
    }

    public static Address toAddressEntity(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        return new Address(dto.street(), dto.city(), dto.postalCode(), dto.country(), dto.region());
    }
}
