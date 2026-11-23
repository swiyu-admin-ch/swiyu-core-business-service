package ch.admin.bj.swiyu.core.business.modules.trust.api;

import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.vqpsSubmissionCreateRequestDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.bj.swiyu.core.business.modules.trust.config.VqpsSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DcqlQueryValidationFailedException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** DCQL {@code query} validation. HTTP integration: {@code VqpsSubmissionB2BControllerIT}. */
class DcqlQueryValidatorTest {

    private static final int MAX_QUERY_LENGTH = 100_000;

    private static final ObjectMapper OBJECT_MAPPER = tools.jackson.databind.json.JsonMapper.builder()
        .findAndAddModules()
        .build();

    private DcqlQueryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DcqlQueryValidator(
            JsonMapper.builder().build(),
            Validation.buildDefaultValidatorFactory().getValidator(),
            new VqpsSubmissionLimitProperties(new VqpsSubmissionLimitProperties.DcqlQueryLimits(MAX_QUERY_LENGTH))
        );
    }

    @Test
    void validateDcqlQuery_withValidQuery_doesNotThrow() {
        assertThatCode(() ->
            validator.validateDcqlQuery(vqpsSubmissionCreateRequestDto().query())
        ).doesNotThrowAnyException();
    }

    @Test
    void validateDcqlQuery_withMinimalValidQuery_doesNotThrow() throws JacksonException {
        var queryJson = """
            {
              "credentials": [
                {
                  "meta": {
                    "vct_values": ["https://credentials.example.com/identity_credential"]
                  }
                }
              ]
            }
            """;
        var query = OBJECT_MAPPER.readTree(queryJson);

        assertThatCode(() -> validator.validateDcqlQuery(query)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            """
            {"credentials": []}
            """,
            """
            {"credentials": [{"meta": {"vct_values": []}}]}
            """,
            """
            {"credentials": [{}]}
            """,
            """
            {"credentials": [{"meta": {}}]}
            """,
            """
            null
            """,
        }
    )
    void validateVqpsSubmission_withInvalidDcqlStructure_throwsDcqlQueryValidationFailedException(String queryJson)
        throws JacksonException {
        var query = OBJECT_MAPPER.readTree(queryJson);

        assertThatThrownBy(() -> validator.validateDcqlQuery(query)).isInstanceOf(
            DcqlQueryValidationFailedException.class
        );
    }

    @Test
    void validateVqpsSubmission_withCredentialsNotAnArray_throwsDcqlQueryValidationFailedException()
        throws JacksonException {
        var queryJson = """
            {"credentials": "not-an-array"}
            """;
        var query = OBJECT_MAPPER.readTree(queryJson);

        assertThatThrownBy(() -> validator.validateDcqlQuery(query))
            .isInstanceOf(DcqlQueryValidationFailedException.class)
            .hasMessageContaining("vqPS submission request is invalid")
            .hasMessageContaining("Invalid JSON in query");
    }

    @Test
    void validateDcqlQuery_withQueryAtMaxLength_doesNotThrow() throws JacksonException {
        var query = queryOfLength(MAX_QUERY_LENGTH);

        assertThat(OBJECT_MAPPER.writeValueAsString(query)).hasSize(MAX_QUERY_LENGTH);
        assertThatCode(() -> validator.validateDcqlQuery(query)).doesNotThrowAnyException();
    }

    @Test
    void validateDcqlQuery_withQueryExceedingMaxLength_throwsDcqlQueryValidationFailedException()
        throws JacksonException {
        var query = queryOfLength(MAX_QUERY_LENGTH + 1);

        assertThatThrownBy(() -> validator.validateDcqlQuery(query))
            .isInstanceOf(DcqlQueryValidationFailedException.class)
            .hasMessageContaining("must not exceed 100000 characters");
    }

    @Test
    void validateDcqlQuery_withWhitespacePadding_doesNotCountTowardsMaxLength() throws JacksonException {
        var queryJson =
            "  ".repeat(MAX_QUERY_LENGTH / 2) +
            """
                {"credentials":[{"meta":{"vct_values":["https://credentials.example.com/identity_credential"]}}]}
                """ +
            "  ".repeat(MAX_QUERY_LENGTH / 2);
        assertThat(queryJson).hasSizeGreaterThan(MAX_QUERY_LENGTH);
        var query = OBJECT_MAPPER.readTree(queryJson);

        assertThatCode(() -> validator.validateDcqlQuery(query)).doesNotThrowAnyException();
    }

    /**
     * Builds a structurally valid DCQL query whose compact serialization has exactly the given length in characters.
     */
    private static JsonNode queryOfLength(int length) throws JacksonException {
        var emptyQuery = OBJECT_MAPPER.readTree(
            """
            {"credentials":[{"meta":{"vct_values":[""]}}]}
            """
        );
        var overhead = OBJECT_MAPPER.writeValueAsString(emptyQuery).length();
        var padding = "x".repeat(length - overhead);
        return OBJECT_MAPPER.readTree("{\"credentials\":[{\"meta\":{\"vct_values\":[\"" + padding + "\"]}}]}");
    }
}
