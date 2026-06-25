package ch.admin.bj.swiyu.core.business.common.i18n;

import static ch.admin.bj.swiyu.core.business.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidLocalizedMapValidator implements ConstraintValidator<ValidLocalizedMap, Map<String, String>> {

    /**
     * Matches BCP 47 language tags in the form: language[-script][-region]
     * <ul>
     *   <li>language: 2-3 alpha chars (e.g. "de", "zh", "rm")</li>
     *   <li>script: optional 4 alpha chars (e.g. "Hans")</li>
     *   <li>region: optional 2 alpha or 3 digit chars (e.g. "CH", "419")</li>
     * </ul>
     * Examples: {@code de}, {@code de-CH}, {@code zh-Hans}, {@code zh-Hans-CN}, {@code rm-CH}
     */
    private static final Pattern ALLOWED_BCP_47_LOCALES_REGEX = Pattern.compile(
        "^[a-zA-Z]{2,3}(-[a-zA-Z]{4})?(-([a-zA-Z]{2}|\\d{3}))?$"
    );

    @Override
    public boolean isValid(Map<String, String> map, ConstraintValidatorContext context) {
        try {
            validateLocalizedMap(map);
            return true;
        } catch (ValidationException e) {
            log.debug("invalid localized map submitted: {}", e.getMessage());
            return false;
        }
    }

    public static void validateLocalizedMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            throw new ValidationException("Map cannot be null or empty");
        }
        validateContainsDefault(map);
        validateLocaleKeys(map);
    }

    private static void validateLocaleKeys(Map<String, String> map) {
        map
            .keySet()
            .stream()
            .filter(key -> !DEFAULT_VALUE_KEY.equals(key))
            .filter(key -> !ALLOWED_BCP_47_LOCALES_REGEX.matcher(key).matches())
            .findFirst()
            .ifPresent(key -> {
                throw new ValidationException("Invalid locale key: " + key);
            });
    }

    private static void validateContainsDefault(Map<String, String> map) {
        if (!map.containsKey(DEFAULT_VALUE_KEY)) {
            throw new ValidationException("Map must contain a \"" + DEFAULT_VALUE_KEY + "\" language entry");
        }
    }
}
