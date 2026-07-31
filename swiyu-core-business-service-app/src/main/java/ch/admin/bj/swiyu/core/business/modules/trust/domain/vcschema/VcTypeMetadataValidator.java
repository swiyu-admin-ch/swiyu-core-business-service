package ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema;

import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustRegistryProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.VcTypeMetadataValidationFailedException;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@AllArgsConstructor
public class VcTypeMetadataValidator {

    private final JsonMapper schemaValidatorObjectMapper;
    private final JsonSchema vcTypeMetadataSchema;
    private final TrustRegistryProperties trustRegistryProperties;

    public void validateVcTypeMetadata(String vcTypeMetadata) throws VcTypeMetadataValidationFailedException {
        if (vcTypeMetadata == null || vcTypeMetadata.isEmpty()) {
            throw new VcTypeMetadataValidationFailedException("VCTypeMetadata is null or empty", null);
        }

        // basic schema validation
        Set<ValidationMessage> validationResult;
        try {
            validationResult = vcTypeMetadataSchema.validate(vcTypeMetadata, InputFormat.JSON);
        } catch (RuntimeException e) {
            throw new VcTypeMetadataValidationFailedException(e.getMessage(), e);
        }
        if (!validationResult.isEmpty()) {
            throw new VcTypeMetadataValidationFailedException(validationResult.toString(), null);
        }

        JsonNode vcTypeMetadataJson;
        try {
            vcTypeMetadataJson = schemaValidatorObjectMapper.readTree(vcTypeMetadata);
        } catch (JacksonException e) {
            throw new VcTypeMetadataValidationFailedException(e.getMessage(), e);
        }

        String vct = vcTypeMetadataJson.get("vct").stringValue();
        String trustBaseUrl = trustRegistryProperties.dataServiceBaseUrl().toString();
        if (!vct.startsWith(trustBaseUrl)) {
            throw new VcTypeMetadataValidationFailedException("vct must start with: " + trustBaseUrl, null);
        }
    }
}
