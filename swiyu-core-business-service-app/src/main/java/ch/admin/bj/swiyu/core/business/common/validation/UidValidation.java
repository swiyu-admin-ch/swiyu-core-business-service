package ch.admin.bj.swiyu.core.business.common.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UidValidation {

    // Same SWISS_UID_PATTERN is used in swiyu-ecosystem-portal. Must be kept in sync.
    public static final String SWISS_UID_PATTERN = "^CHE(?:-?\\d{3}\\.\\d{3}\\.\\d{3}|\\d{9})$";
}
