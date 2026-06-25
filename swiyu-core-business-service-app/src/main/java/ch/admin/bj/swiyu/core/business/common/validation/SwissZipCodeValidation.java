package ch.admin.bj.swiyu.core.business.common.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SwissZipCodeValidation {

    // Same pattern is used in swiyu-ecosystem-portal. Must be kept in sync.
    // Pattern: 4-digit Swiss postal code starting with a non-zero digit (1000–9999)
    public static final String SWISS_ZIP_CODE_PATTERN = "^[1-9]\\d{3}$";
}
