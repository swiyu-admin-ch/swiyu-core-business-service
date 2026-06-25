package ch.admin.bj.swiyu.core.business.modules.management.api;

import static ch.admin.bj.swiyu.core.business.common.validation.EmailValidation.EMAIL_REGEX;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateBusinessEntity")
public record CreateBusinessEntityDto(
    @NotBlank
    @Size(max = 45)
    @Pattern(regexp = "[^\\x00-\\x1F\\x7F]*") // Control (non-printable) characters are not allowed
    @Schema(
        description = "Unique name of the entity to be created. Can not be changed once created. Allowed are letters, digits, spaces, and most special characters.",
        example = "John Doe"
    )
    String name,
    @NotBlank
    @Pattern(regexp = EMAIL_REGEX) // // Same RFC2822 pattern is used in swiyu-ecosystem-portal. Must be kept in sync.
    @Schema(
        description = "Email address how the controller of the entity can be reached. Ideally a shared mailbox.",
        example = "example@example.com"
    )
    String contactEmailAddress,
    @NotNull
    @Schema(description = "Type of the business partner.", example = "GOVERNMENTAL_INSTITUTION")
    BusinessPartnerTypeDto type
) {
    public CreateBusinessEntityDto(String name, String contactEmailAddress) {
        this(name, contactEmailAddress, null);
    }
}
