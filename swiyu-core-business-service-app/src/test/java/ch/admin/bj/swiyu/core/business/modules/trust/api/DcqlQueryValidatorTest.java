package ch.admin.bj.swiyu.core.business.modules.trust.api;

import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.vqpsSubmissionCreateRequestDto;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DcqlQueryValidationFailedException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** DCQL {@code query} validation. HTTP integration: {@code VqpsSubmissionB2BControllerIT}. */
class DcqlQueryValidatorTest {

    private static final ObjectMapper OBJECT_MAPPER = tools.jackson.databind.json.JsonMapper.builder()
        .findAndAddModules()
        .build();

    private DcqlQueryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DcqlQueryValidator(
            JsonMapper.builder().build(),
            Validation.buildDefaultValidatorFactory().getValidator()
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
}
