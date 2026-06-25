package ch.admin.bj.swiyu.core.business.common.domain;

import ch.admin.bj.swiyu.core.business.common.validation.ValidPhone;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Embeddable
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class Contact {

    String firstName;
    String lastName;
    String email;

    @NotBlank
    @ValidPhone
    String phone;

    @Embedded
    Address address;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
