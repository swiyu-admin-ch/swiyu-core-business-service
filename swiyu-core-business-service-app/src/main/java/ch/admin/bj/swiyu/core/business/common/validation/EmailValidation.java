package ch.admin.bj.swiyu.core.business.common.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EmailValidation {

    // Same RFC2822 pattern is used in swiyu-ecosystem-portal. Must be kept in sync.
    public static final String EMAIL_REGEX =
        "^[a-zA-Z0-9.!#$%&'*+=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
}
