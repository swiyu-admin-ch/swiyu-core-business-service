package ch.admin.bj.swiyu.core.business.common.domain;

import ch.admin.bj.swiyu.core.business.common.validation.ValidPhone;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class Contact {

    String firstName;
    String lastName;
    String email;

    // Phone is optional at the domain level — BusinessEntity contacts may not have a phone.
    // Validation of mandatory phone is enforced at the API layer via ContactDto.
    @ValidPhone
    String phone;

    @Enumerated(EnumType.STRING)
    Language correspondingLanguage;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // EID-6624 this needs documentation - why are contacts so often created without the other fields
    public static Contact of(String email, String phone) {
        return new Contact(null, null, email, phone, null);
    }
}
