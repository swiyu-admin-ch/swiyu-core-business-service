package ch.admin.bj.swiyu.core.business.modules.management.api;

import static ch.admin.bj.swiyu.core.business.common.validation.UidValidation.SWISS_UID_PATTERN;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "BusinessPartnerUpdate")
public record BusinessPartnerUpdateDto(
    @Schema(description = "Self-declared organisation name. Only updatable when identity is not yet ACTIVE.")
    @Size(max = 45)
    @Pattern(regexp = "[^\\x00-\\x1F\\x7F]*")
    String name,

    @Schema(description = "Enterprise identification number. Only updatable when identity is not yet ACTIVE.")
    @Pattern(regexp = SWISS_UID_PATTERN)
    String uid,

    @Schema(description = "Address of the organisation") @Valid AddressDto address,

    @Schema(description = "Contact person details") @Valid ContactDto contact
) {}
