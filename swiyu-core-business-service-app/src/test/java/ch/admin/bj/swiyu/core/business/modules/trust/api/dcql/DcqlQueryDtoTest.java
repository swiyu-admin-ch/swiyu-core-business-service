package ch.admin.bj.swiyu.core.business.modules.trust.api.dcql;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DcqlQueryDtoTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final jakarta.validation.Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deserialize_preservesUnknownCredentialFieldsInAdditionalProperties() throws Exception {
        var json = """
            {
              "credentials": [
                {
                  "id": "my_credential",
                  "format": "dc+sd-jwt",
                  "meta": {
                    "vct_values": ["https://credentials.example.com/identity_credential"]
                  },
                  "claims": [{"path": ["last_name"]}]
                }
              ],
              "credential_sets": [{"options": [["my_credential"]], "required": true}]
            }
            """;

        var dto = jsonMapper.readValue(json, DcqlQueryDto.class);

        assertThat(dto.credentials()).hasSize(1);
        var credential = dto.credentials().getFirst();
        assertThat(credential.meta().vctValues()).containsExactly(
            "https://credentials.example.com/identity_credential"
        );
        assertThat(credential.additionalProperties())
            .containsEntry("id", "my_credential")
            .containsEntry("format", "dc+sd-jwt")
            .containsKey("claims");
        assertThat(dto.additionalProperties()).containsKey("credential_sets");
        assertThat(beanValidator.validate(dto)).isEmpty();
    }

    @Test
    void deserialize_metaWithUnknownFields_preservesAdditionalProperties() throws Exception {
        var json = """
            {
              "credentials": [
                {
                  "meta": {
                    "vct_values": ["https://example.com/vct"],
                    "doctype_value": "org.iso.18013.5.1.mDL"
                  }
                }
              ]
            }
            """;

        var dto = jsonMapper.readValue(json, DcqlQueryDto.class);

        assertThat(dto.credentials().getFirst().meta().additionalProperties()).containsEntry(
            "doctype_value",
            "org.iso.18013.5.1.mDL"
        );
        assertThat(beanValidator.validate(dto)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "[]", "[\"\"]", "[null]" })
    void beanValidation_failsWhenVctValuesEmpty(String vctValues) throws Exception {
        var json = """
            { "credentials": [ { "meta": { "vct_values": %s } } ] }
            """.formatted(vctValues);

        var dto = jsonMapper.readValue(json, DcqlQueryDto.class);

        assertThat(beanValidator.validate(dto)).isNotEmpty();
    }
}
