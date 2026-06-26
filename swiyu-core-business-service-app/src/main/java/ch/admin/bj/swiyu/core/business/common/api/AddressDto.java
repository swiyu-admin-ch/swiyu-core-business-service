package ch.admin.bj.swiyu.core.business.common.api;

import static ch.admin.bj.swiyu.core.business.common.validation.SwissZipCodeValidation.SWISS_ZIP_CODE_PATTERN;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
@Schema(name = "Address")
public record AddressDto(
    String street,
    @NotBlank String city,
    @NotBlank @Pattern(regexp = SWISS_ZIP_CODE_PATTERN) String postalCode,
    @NotBlank String country,
    // To be removed with EID-6303
    String region
) {}
