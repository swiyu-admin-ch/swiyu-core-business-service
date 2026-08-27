package ch.admin.bj.swiyu.core.business.modules.management.api;

import static ch.admin.bj.swiyu.core.business.common.validation.UidValidation.SWISS_UID_PATTERN;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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

    @NotNull @Valid @Schema(description = "The official address of the partner.") AddressDto address,

    @NotNull @Valid @Schema(description = "The contact person for the partner.") ContactDto contact
) {}
