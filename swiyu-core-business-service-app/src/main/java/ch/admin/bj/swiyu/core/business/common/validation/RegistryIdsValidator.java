package ch.admin.bj.swiyu.core.business.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.regex.Pattern;

public class RegistryIdsValidator implements ConstraintValidator<ValidRegistryIds, Map<String, String>> {

    private static final Pattern UID_PATTERN = Pattern.compile(UidValidation.SWISS_UID_PATTERN);
    private static final String UID_KEY = "UID";

    @Override
    public boolean isValid(Map<String, String> value, ConstraintValidatorContext context) {
        if (value == null || !value.containsKey(UID_KEY)) {
            return true;
        }

        var uid = value.get(UID_KEY);
        if (uid == null || uid.isBlank()) {
            return true;
        }

        return UID_PATTERN.matcher(uid).matches();
    }
}
