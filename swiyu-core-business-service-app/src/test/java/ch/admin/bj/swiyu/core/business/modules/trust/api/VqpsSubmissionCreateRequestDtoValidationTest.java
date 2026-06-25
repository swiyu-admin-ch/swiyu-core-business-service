package ch.admin.bj.swiyu.core.business.modules.trust.api;

import static ch.admin.bj.swiyu.core.business.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;
import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VqpsSubmissionCreateRequestDtoValidationTest {

    private static Validator beanValidator;

    @BeforeAll
    static void setUpValidator() {
        beanValidator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    class HappyPath {

        @Test
        void fullyValidRequest_passesBeanValidation() {
            assertThat(beanValidator.validate(vqpsSubmissionCreateRequestDto())).isEmpty();
        }

        @Test
        void purposeNameAtMaxLength40_isValid() {
            var request = withPurposeName(textOfLength(40), Map.of("de-CH", textOfLength(40)));

            assertThat(beanValidator.validate(request)).isEmpty();
        }

        @Test
        void purposeDescriptionAtMaxLength1000_isValid() {
            var request = withPurposeDescription(textOfLength(1000), Map.of("de-CH", textOfLength(1000)));

            assertThat(beanValidator.validate(request)).isEmpty();
        }
    }

    @Nested
    class Sub {

        @Test
        void nonBlankSub_isValid() {
            var request = withSub(
                "did:tdw:DEADBEEF0000000000000000000000000000000000000000000000000000000000000000000000000000000000000:identifier-data-service-d.bit.admin.ch:api:v1:did:00000000-0000-0000-0000-000000000000"
            );

            assertThat(beanValidator.validate(request)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   " })
        void blankSub_isInvalid(String sub) {
            var request = withSub(sub);

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }
    }

    @Nested
    class Scope {

        @Test
        void nonBlankScope_isValid() {
            var request = withScope("com.example.identityCardCredential_presentation");

            assertThat(beanValidator.validate(request)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   " })
        void blankScope_isInvalid(String scope) {
            var request = withScope(scope);

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }
    }

    @Nested
    class PurposeName {

        @Test
        void defaultAtMaxLength40_isValid() {
            var request = withPurposeName(textOfLength(40), Map.of());

            assertThat(beanValidator.validate(request)).isEmpty();
        }

        @Test
        void defaultExceedingMaxLength41_isInvalid() {
            var request = withPurposeName(textOfLength(41), Map.of());

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }

        @Test
        void missingDefaultKey_isInvalid() {
            var request = withPurposeNameMap(Map.of("de-CH", "purpose name de"));

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   " })
        void blankDefault_isInvalid(String defaultValue) {
            var request = withPurposeName(defaultValue, Map.of());

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }

        @Test
        void localizedValueAtMaxLength40_isValid() {
            var request = withPurposeName("name", Map.of("de-CH", textOfLength(40)));

            assertThat(beanValidator.validate(request)).isEmpty();
        }

        @Test
        void localizedValueExceedingMaxLength41_isInvalid() {
            var request = withPurposeName("name", Map.of("de-CH", textOfLength(41)));

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }

        @Test
        void invalidLocaleKey_isInvalid() {
            var request = withPurposeNameMap(Map.of(DEFAULT_VALUE_KEY, "name", "cheese", "cake"));

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }
    }

    @Nested
    class PurposeDescription {

        @Test
        void defaultAtMaxLength1000_isValid() {
            var request = withPurposeDescription(textOfLength(1000), Map.of());

            assertThat(beanValidator.validate(request)).isEmpty();
        }

        @Test
        void defaultExceedingMaxLength1001_isInvalid() {
            var request = withPurposeDescription(textOfLength(1001), Map.of());

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   " })
        void blankDefault_isInvalid(String defaultValue) {
            var request = withPurposeDescription(defaultValue, Map.of());

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }

        @Test
        void localizedValueAtMaxLength1000_isValid() {
            var request = withPurposeDescription("desc", Map.of("fr-CH", textOfLength(1000)));

            assertThat(beanValidator.validate(request)).isEmpty();
        }

        @Test
        void localizedValueExceedingMaxLength1001_isInvalid() {
            var request = withPurposeDescription("desc", Map.of("fr-CH", textOfLength(1001)));

            assertThat(beanValidator.validate(request)).isNotEmpty();
        }
    }

    private static VqpsSubmissionCreateRequestDto withSub(String sub) {
        var base = vqpsSubmissionCreateRequestDto();
        return vqpsSubmissionCreateRequestDto(
            sub,
            base.purposeName(),
            base.purposeDescription(),
            base.scope(),
            base.query()
        );
    }

    private static VqpsSubmissionCreateRequestDto withScope(String scope) {
        var base = vqpsSubmissionCreateRequestDto();
        return vqpsSubmissionCreateRequestDto(
            base.sub(),
            base.purposeName(),
            base.purposeDescription(),
            scope,
            base.query()
        );
    }

    private static VqpsSubmissionCreateRequestDto withPurposeName(String defaultValue, Map<String, String> localized) {
        var base = vqpsSubmissionCreateRequestDto();
        return vqpsSubmissionCreateRequestDto(
            base.sub(),
            localizedMap(defaultValue, localized),
            base.purposeDescription(),
            base.scope(),
            base.query()
        );
    }

    private static VqpsSubmissionCreateRequestDto withPurposeNameMap(Map<String, String> purposeName) {
        var base = vqpsSubmissionCreateRequestDto();
        return vqpsSubmissionCreateRequestDto(
            base.sub(),
            purposeName,
            base.purposeDescription(),
            base.scope(),
            base.query()
        );
    }

    private static VqpsSubmissionCreateRequestDto withPurposeDescription(
        String defaultValue,
        Map<String, String> localized
    ) {
        var base = vqpsSubmissionCreateRequestDto();
        return vqpsSubmissionCreateRequestDto(
            base.sub(),
            base.purposeName(),
            localizedMap(defaultValue, localized),
            base.scope(),
            base.query()
        );
    }

    private static String textOfLength(int length) {
        return "x".repeat(length);
    }
}
