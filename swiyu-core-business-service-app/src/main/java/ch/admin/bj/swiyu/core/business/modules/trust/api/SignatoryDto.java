package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.validation.EmailValidation;
import ch.admin.bj.swiyu.core.business.common.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "Signatory")
public record SignatoryDto(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @ValidPhone String phone,
    @NotBlank @Pattern(regexp = EmailValidation.EMAIL_REGEX) String email
) {}
