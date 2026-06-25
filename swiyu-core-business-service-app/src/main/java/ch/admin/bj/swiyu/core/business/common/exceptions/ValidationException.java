package ch.admin.bj.swiyu.core.business.common.exceptions;

import java.util.List;
import lombok.Getter;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

@Getter
public class ValidationException extends RuntimeException {

    private final String code = "VALIDATION_ERROR";
    private final transient List<Violation> violations;

    public ValidationException(String message, Errors errors) {
        super(message + ": " + errors.getAllErrors());
        this.violations = errors
            .getAllErrors()
            .stream()
            .map(e -> new Violation(e instanceof FieldError f ? f.getField() : e.getObjectName(), e.getCode()))
            .toList();
    }

    public record Violation(String path, String reason) {}
}
