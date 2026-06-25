package ch.admin.bj.swiyu.core.business.modules.management.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S1874")
class BusinessEntityValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void contactEmail_invalidFormat_failsValidation() {
        var entity = new BusinessEntity(
            UUID.randomUUID(),
            "Valid Name",
            "invalid-email@",
            BusinessPartnerType.BUSINESS
        );

        var violations = validator.validate(entity);

        assertThat(violations)
            .extracting(v -> v.getPropertyPath().toString())
            .filteredOn("contactEmail"::equals)
            .isNotEmpty();
    }

    @Test
    void contactEmail_validFormat_passesValidation() {
        var entity = new BusinessEntity(
            UUID.randomUUID(),
            "Valid Name",
            "valid.email@example.com",
            BusinessPartnerType.BUSINESS
        );

        var violations = validator.validate(entity);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
            .extracting(v -> v.getPropertyPath().toString())
            .doesNotContain("contactEmail");
    }
}
