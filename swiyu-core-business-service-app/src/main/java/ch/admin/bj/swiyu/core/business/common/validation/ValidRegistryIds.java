package ch.admin.bj.swiyu.core.business.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RegistryIdsValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRegistryIds {
    String message() default "registryIds contains an invalid value: the 'UID' entry must match the Swiss UID format (e.g. CHE-123.456.789)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
