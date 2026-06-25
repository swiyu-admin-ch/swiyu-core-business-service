package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import ch.admin.bj.swiyu.core.business.common.validation.EmailValidation;
import ch.admin.bj.swiyu.core.business.common.validation.ValidPhone;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Signatory(
    @NotBlank String firstName,

    @NotBlank String lastName,

    @NotBlank @ValidPhone String phone,

    @NotBlank @Pattern(regexp = EmailValidation.EMAIL_REGEX) String email
) {
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
