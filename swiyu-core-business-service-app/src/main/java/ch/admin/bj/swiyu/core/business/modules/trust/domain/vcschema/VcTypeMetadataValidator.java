package ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema;

import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustRegistryProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.VcTypeMetadataValidationFailedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.networknt.schema.JsonSchema;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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

        try {
            JsonNode vcTypeMetadataJson = schemaValidatorObjectMapper.readTree(vcTypeMetadata);
            // basic schema validation
            var validationResult = vcTypeMetadataSchema.validate(vcTypeMetadataJson);
            if (!validationResult.isEmpty()) {
                throw new VcTypeMetadataValidationFailedException(validationResult.toString(), null);
            }

            String vct = vcTypeMetadataJson.get("vct").asText();
            String trustBaseUrl = trustRegistryProperties.dataServiceBaseUrl().toString();
            if (!vct.startsWith(trustBaseUrl)) {
                throw new VcTypeMetadataValidationFailedException("vct must start with: " + trustBaseUrl, null);
            }
        } catch (JsonProcessingException e) {
            throw new VcTypeMetadataValidationFailedException(e.getMessage(), e);
        }
    }
}
