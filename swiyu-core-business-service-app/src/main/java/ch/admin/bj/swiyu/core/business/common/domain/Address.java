package ch.admin.bj.swiyu.core.business.common.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class Address {

    private String street;
    private String city;
    private String postalCode;
    private String country;
    private String region;

    public String getFullAddressOneLine() {
        return String.format("%s, %s %s, %s", street, postalCode, city, country);
    }
}
