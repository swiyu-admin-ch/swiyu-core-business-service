package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.modules.trust.api.dcql.DcqlQueryDto;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DcqlQueryValidationFailedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DcqlQueryValidator {

    private final JsonMapper schemaValidatorObjectMapper;
    private final Validator validator;

    public void validateDcqlQuery(JsonNode dcqlQueryJson) {
        if (dcqlQueryJson == null || dcqlQueryJson.isNull()) {
            throw new DcqlQueryValidationFailedException("query must not be null");
        }

        DcqlQueryDto dcqlQuery;
        try {
            dcqlQuery = schemaValidatorObjectMapper.treeToValue(dcqlQueryJson, DcqlQueryDto.class);
        } catch (JsonProcessingException e) {
            throw new DcqlQueryValidationFailedException("Invalid JSON in query: " + e.getMessage(), e);
        }

        var violations = validator.validate(dcqlQuery);
        if (!violations.isEmpty()) {
            var details = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("; "));
            throw new DcqlQueryValidationFailedException("Invalid DCQL query: " + details);
        }
    }
}
