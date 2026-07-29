package ch.admin.bj.swiyu.core.business.common.api;

import ch.admin.bj.swiyu.core.business.common.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(name = "Contact")
public record ContactDto(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank String email,
    @NotBlank @ValidPhone String phone,
    LanguageDto correspondingLanguage,

    @SuppressWarnings("java:S1133") // remove with EID-6303
    @Deprecated(since = "3.42.5")
    @Schema(deprecated = true)
    AddressDto address
) {}
