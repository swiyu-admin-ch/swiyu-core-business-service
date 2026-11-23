package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.modules.trust.api.dcql.DcqlQueryDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.VqpsSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DcqlQueryValidationFailedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@AllArgsConstructor
public class DcqlQueryValidator {

    private final JsonMapper schemaValidatorObjectMapper;
    private final Validator validator;
    private final VqpsSubmissionLimitProperties limitProperties;

    public void validateDcqlQuery(JsonNode dcqlQueryJson) {
        if (dcqlQueryJson == null || dcqlQueryJson.isNull()) {
            throw new DcqlQueryValidationFailedException("query must not be null");
        }

        validateMaxLength(dcqlQueryJson);

        DcqlQueryDto dcqlQuery;
        try {
            dcqlQuery = schemaValidatorObjectMapper.treeToValue(dcqlQueryJson, DcqlQueryDto.class);
        } catch (JacksonException e) {
            throw new DcqlQueryValidationFailedException("Invalid JSON in query: " + e.getMessage(), e);
        }

        var violations = validator.validate(dcqlQuery);
        if (!violations.isEmpty()) {
            var details = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("; "));
            throw new DcqlQueryValidationFailedException("Invalid DCQL query: " + details);
        }
    }

    /**
     * The length is measured on the compact serialization of the query, as this is what gets stored and published to
     * the trust registry. Whitespace used to format the query on the client side therefore does not count towards the
     * limit. The length is measured in characters (UTF-16 code units).
     */
    private void validateMaxLength(JsonNode dcqlQueryJson) {
        var maxLength = limitProperties.dcqlQuery().maxLength();
        var queryLength = schemaValidatorObjectMapper.writeValueAsString(dcqlQueryJson).length();
        if (queryLength > maxLength) {
            throw new DcqlQueryValidationFailedException(
                "Invalid DCQL query: query must not exceed " + maxLength + " characters, but has " + queryLength
            );
        }
    }
}
