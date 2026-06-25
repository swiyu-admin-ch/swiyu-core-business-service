package ch.admin.bj.swiyu.core.business.common.i18n;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidLocalizedMapValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLocalizedMap {
    String message() default "Map contains invalid locale key(s)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
