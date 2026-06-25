package ch.admin.bj.swiyu.core.business.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PhoneNumberConditionalValidator implements ConstraintValidator<ValidPhone, String> {

    private static final Pattern SWISS_PATTERN = Pattern.compile(PhoneValidation.SWISS_PHONE_PATTERN_E164_STYLE);
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile(
        PhoneValidation.INTERNATIONAL_PHONE_PATTERN_E164_STYLE
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String normalizedValue = PhoneValidation.normalizePhoneNumber(value);

        if (normalizedValue.startsWith("+41")) {
            return SWISS_PATTERN.matcher(normalizedValue).matches();
        }

        return INTERNATIONAL_PATTERN.matcher(normalizedValue).matches();
    }
}
