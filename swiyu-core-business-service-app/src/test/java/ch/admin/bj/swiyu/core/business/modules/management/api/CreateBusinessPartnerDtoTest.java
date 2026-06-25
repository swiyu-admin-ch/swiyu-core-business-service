package ch.admin.bj.swiyu.core.business.modules.management.api;

import static org.junit.jupiter.api.Assertions.*;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CreateBusinessPartnerDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    class NameTests {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "123!@#$%^&*()?£^`¢[]{};:'\",<.>/\\|_~+_≠≤≥≈©™∞",
                "株式会社日本革新",
                "未来科技有限公司",
                "Κέντρο Καινοτομίας",
            }
        )
        void testValidInput(String name) {
            var createBusinessEntityDto = new CreateBusinessEntityDto(
                name,
                "test@example.com",
                BusinessPartnerTypeDto.BUSINESS
            );
            var violations = validator.validate(createBusinessEntityDto);

            // Assert no violations
            assertTrue(violations.isEmpty(), "Valid input should not cause validation errors.");
        }

        @ParameterizedTest
        @ValueSource(strings = { "Hello\nWorld", "Hello\u007FWorld", "Hello\u0001World" })
        void testInvalidCharacterInput(String name) {
            var createBusinessEntityDto = new CreateBusinessEntityDto(name, "test@example.com");
            var violations = validator.validate(createBusinessEntityDto);

            // Assert there is a violation
            assertFalse(violations.isEmpty(), "Invalid input due to invalid character should cause validation errors.");
        }

        @Test
        void testInvalidLengthInput() {
            var invalidBusinessName =
                "This is a business name with more than 45 characters so this should result in a validation error";
            var createBusinessEntityDto = new CreateBusinessEntityDto(invalidBusinessName, "test@example.com");
            var violations = validator.validate(createBusinessEntityDto);

            // Assert there is a violation
            assertFalse(
                violations.isEmpty(),
                "Invalid input due to exceeding max characters should cause validation errors."
            );
        }

        @Test
        void testEmptyInput() {
            var createBusinessEntityDto = new CreateBusinessEntityDto("", "test@example.com");
            var violations = validator.validate(createBusinessEntityDto);

            // Assert there is a violation
            assertFalse(violations.isEmpty(), "Empty input should cause validation errors.");
        }
    }

    @Nested
    class EmailTests {

        @Test
        void testValidInput() {
            var createBusinessEntityDto = new CreateBusinessEntityDto(
                "Valid Name",
                "test@example.com",
                BusinessPartnerTypeDto.BUSINESS
            );
            var violations = validator.validate(createBusinessEntityDto);

            // Assert no violations
            assertTrue(violations.isEmpty(), "Valid email input should not cause validation errors.");
        }

        @Test
        void testInvalidInput() {
            var createBusinessEntityDto = new CreateBusinessEntityDto("Valid Name", "invalid-email@");
            var violations = validator.validate(createBusinessEntityDto);

            // Assert there is a violation
            assertFalse(violations.isEmpty(), "Invalid email input should cause validation errors.");
        }

        @Test
        void testEmptyInput() {
            var createBusinessEntityDto = new CreateBusinessEntityDto("Valid Name", "");
            var violations = validator.validate(createBusinessEntityDto);

            // Assert there is a violation
            assertFalse(violations.isEmpty(), "Empty email input should cause validation errors.");
        }
    }
}
