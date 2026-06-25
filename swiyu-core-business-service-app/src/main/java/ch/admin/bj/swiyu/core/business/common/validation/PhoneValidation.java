package ch.admin.bj.swiyu.core.business.common.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PhoneValidation {

    // Normalized Swiss phone number: +41 followed by exactly 9 digits.
    public static final String SWISS_PHONE_PATTERN_E164_STYLE = "^\\+41[1-9]\\d{8}$";

    // Normalized international phone number: + followed by 7 to 15 digits.
    public static final String INTERNATIONAL_PHONE_PATTERN_E164_STYLE = "^\\+[1-9]\\d{6,14}$";

    public static String normalizePhoneNumber(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("[\\s().-]", "").replaceFirst("^00", "+");
    }
}
