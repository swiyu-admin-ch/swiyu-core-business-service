package ch.admin.bj.swiyu.core.business.common.api;

import static ch.admin.bj.swiyu.core.business.common.validation.EmailValidation.EMAIL_REGEX;

import ch.admin.bj.swiyu.core.business.common.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
@Schema(name = "Contact")
public record ContactDto(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Pattern(regexp = EMAIL_REGEX) String email,
    @NotBlank @ValidPhone String phone,
    LanguageDto correspondingLanguage,

    @SuppressWarnings("java:S1133") // remove with EID-6303
    @Deprecated(since = "3.42.5")
    @Schema(deprecated = true)
    AddressDto address
) {}
