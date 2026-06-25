package ch.admin.bj.swiyu.core.business.modules.management.api;

import static ch.admin.bj.swiyu.core.business.common.validation.EmailValidation.EMAIL_REGEX;
import static ch.admin.bj.swiyu.core.business.common.validation.SwissZipCodeValidation.SWISS_ZIP_CODE_PATTERN;
import static ch.admin.bj.swiyu.core.business.common.validation.UidValidation.SWISS_UID_PATTERN;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "CreatePartner")
public record CreatePartnerDto(
    @NotBlank
    @Size(max = 45)
    @Pattern(regexp = "[^\\x00-\\x1F\\x7F]*") // Control (non-printable) characters are not allowed
    @Schema(
        description = "Unique name of the partner to be created. Can not be changed once created. Allowed are letters, digits, spaces, and most special characters.",
        example = "John Doe"
    )
    String name,

    @NotNull
    @Schema(description = "Type of the business partner.", example = "GOVERNMENTAL_INSTITUTION")
    BusinessPartnerTypeDto partnerType,
    @Pattern(regexp = SWISS_UID_PATTERN)
    @Schema(
        description = "Swiss UID of the partner (e.g. CHE-123.456.789). Optional, validated when present.",
        example = "CHE-123.456.789"
    )
    String uid,
    @Size(max = 255) String addressStreet,
    @NotBlank
    @Pattern(regexp = SWISS_ZIP_CODE_PATTERN)
    @Schema(description = "Swiss postal code (4 digits, 1000–9999).", example = "3000")
    String addressZipCode,
    @Size(max = 255) @NotBlank String addressCity,
    @Size(max = 255) String addressCountry,
    @Size(max = 255) String addressRegion,
    @NotBlank
    @Schema(
        description = "Phone number under which the controller of the partner can be reached.",
        example = "+41791234567"
    )
    @Size(max = 255)
    @ValidPhone
    String contactPhone,
    @NotBlank
    @Pattern(regexp = EMAIL_REGEX) // // Same RFC2822 pattern is used in swiyu-ecosystem-portal. Must be kept in sync.
    @Schema(
        description = "Email address how the controller of the partner can be reached. Ideally a shared mailbox.",
        example = "example@example.com"
    )
    String contactEmail
) {}
