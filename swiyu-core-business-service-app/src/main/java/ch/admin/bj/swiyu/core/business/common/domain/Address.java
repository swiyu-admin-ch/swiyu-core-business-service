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
        var parts = new java.util.ArrayList<String>();
        if (street != null && !street.isBlank()) {
            parts.add(street);
        }
        var cityPart = java.util.stream.Stream.of(postalCode, city)
            .filter(s -> s != null && !s.isBlank())
            .collect(java.util.stream.Collectors.joining(" "));
        if (!cityPart.isBlank()) {
            parts.add(cityPart);
        }
        if (country != null && !country.isBlank()) {
            parts.add(country);
        }
        return String.join(", ", parts);
    }
}
