package ch.admin.bj.swiyu.core.business.modules.identifier.domain;

import static ch.admin.bj.swiyu.core.business.common.did.DidUtil.detectDidMethod;

import ch.admin.bj.swiyu.core.business.common.did.DidMethod;
import ch.admin.bj.swiyu.core.business.common.did.DidUtil;
import ch.admin.bj.swiyu.core.business.common.exceptions.MaxSizeApiException;
import ch.admin.bj.swiyu.core.business.modules.identifier.config.IdentifierLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.identifier.exceptions.IdentifierValidationFailedException;
import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryProperties;
import ch.admin.bj.swiyu.registry.identifier.service.IdentifierRegistryService;
import ch.admin.eid.did_sidekicks.DidDoc;
import ch.admin.eid.didresolver.DidResolveException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.networknt.schema.JsonSchema;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentifierValidator {

    private final JsonSchema didWebvhSchema;
    private final JsonSchema didTdwSchema;
    private final JsonSchema didDocSchema;
    private final JsonMapper schemaValidatorObjectMapper;
    private final IdentifierLimitProperties identifierLimitProperties;
    private final IdentifierRegistryProperties identifierRegistryProperties;
    private final IdentifierRegistryService identifierRegistryService;

    public void validateDidLog(IdentifierEntry entry, String didLog) throws IdentifierValidationFailedException {
        // Validate that the did log is not empty
        if (didLog.isEmpty()) throw new IdentifierValidationFailedException("No DID Log provided", null);
        var didLogLength = didLog.getBytes().length;
        if (didLogLength > identifierLimitProperties.didLog().maxSize().toBytes()) {
            throw new MaxSizeApiException(identifierLimitProperties.didLog().maxSize(), didLogLength, "DidLog");
        }

        var lines = didLog.lines().toList();
        var method = detectDidMethod(lines.getFirst());
        for (var lineNo = 0; lineNo < lines.size(); lineNo++) {
            var didLogEntryLine = lines.get(lineNo);
            var didLogEntry = validateDidLogEntry(didLogEntryLine, method);

            if (lineNo == 0) {
                try {
                    DidUtil.getScid(didLogEntry);
                } catch (Exception e) {
                    throw new IdentifierValidationFailedException("Initial transaction scid is not available.", e);
                }
            }
        }

        validateDidDoc(entry, didLog);

        if (entry.getUploadCount() > 0) {
            validateDidLogIsNotForked(entry.getId(), didLog);
        }
    }

    private void validateDidLogIsNotForked(UUID identifierId, String didLog) {
        try {
            var registryResponse = identifierRegistryService.getDidTdwFile(identifierId);
            if (!didLog.startsWith(registryResponse)) {
                throw new IdentifierValidationFailedException(
                    "New DIDLog does not start with already uploaded content. Upload prohibited to prevent DID forking.",
                    null
                );
            }
        } catch (RestClientResponseException e) {
            throw new IdentifierValidationFailedException("Identifier could not be found on registry.", e);
        }
    }

    private JsonNode validateDidTdwEntry(JsonNode didLogEntry) {
        // Validate that the did log conforms to the JSON schema
        // and the swiyu limitations.
        var validationResultDidWebvh = didTdwSchema.validate(didLogEntry).stream().toList();

        if (!validationResultDidWebvh.isEmpty()) {
            throw new IdentifierValidationFailedException(validationResultDidWebvh.toString(), null);
        }
        return didLogEntry;
    }

    private JsonNode validateDidWebvhEntry(JsonNode didLogEntry) {
        // Validate that the did log conforms to the JSON schema
        // and the swiyu limitations.
        var validationResultDidWebvh = didWebvhSchema.validate(didLogEntry).stream().toList();

        if (!validationResultDidWebvh.isEmpty()) {
            throw new IdentifierValidationFailedException(validationResultDidWebvh.toString(), null);
        }
        return didLogEntry;
    }

    private JsonNode validateDidLogEntry(String didLogEntryLine, DidMethod typeGuess) {
        JsonNode didLogEntry;
        try {
            // Validate JSON structure
            didLogEntry = schemaValidatorObjectMapper.readTree(didLogEntryLine);
        } catch (JsonProcessingException e) {
            throw new IdentifierValidationFailedException(e.getMessage(), e);
        }

        try {
            return validateDidWebvhEntry(didLogEntry);
        } catch (IdentifierValidationFailedException didWebvhException) {
            try {
                return validateDidTdwEntry(didLogEntry);
            } catch (IdentifierValidationFailedException didTdwException) {
                if (typeGuess == DidMethod.DID_TDW) {
                    throw didTdwException;
                }
                throw didWebvhException;
            }
        }
    }

    private void validateDidDoc(IdentifierEntry entry, String didLog)
        throws IdentifierValidationFailedException, MaxSizeApiException {
        // Validate that the did log conforms to the did:tdw specification
        try {
            var didDoc = DidUtil.getDidDoc(didLog);
            var didDocJson = didDoc.toJson();

            var didDocLength = didDocJson.getBytes().length;
            if (didDocLength > identifierLimitProperties.didDoc().maxSize().toBytes()) {
                throw new MaxSizeApiException(identifierLimitProperties.didDoc().maxSize(), didDocLength, "DidDoc");
            }

            // Validate json of diddoc
            var didDocJsonParsed = schemaValidatorObjectMapper.readTree(didDocJson);

            // Validate that the did doc conforms to the JSON schema
            // and the swiyu limitations.
            var validationResult = didDocSchema.validate(didDocJsonParsed).stream().toList();
            if (!validationResult.isEmpty()) {
                List<String> validationResults = new ArrayList<>();
                validationResult.forEach(result -> validationResults.add(result.getMessage()));
                throw new IdentifierValidationFailedException(validationResults, null);
            }
            validateDidPointsToBaseRegistry(entry, didDoc);

            validateDidIsOnlyControlledByItself(didDocJsonParsed);
        } catch (JsonProcessingException e) {
            throw new IdentifierValidationFailedException("Cannot parse DIDDoc. " + e.getMessage(), e);
        } catch (Exception e) {
            // Generic exception handling is required as the RUST lib throws exceptions
            // which are not checked but should result in this wrapper
            // to not double wrap exception IdentifierValidationFailedException are directly re-thrown
            if (e instanceof IdentifierValidationFailedException ex) {
                throw ex;
            }
            if (e instanceof MaxSizeApiException ex) {
                throw ex;
            }
            throw new IdentifierValidationFailedException(e.getMessage(), e);
        }
    }

    private void validateDidPointsToBaseRegistry(IdentifierEntry entry, DidDoc didDoc) {
        try {
            var url = DidUtil.getDidUrl(didDoc);
            if (
                identifierRegistryProperties
                    .getPublicResolveUrlTemplates()
                    .stream()
                    .noneMatch(allowedUrlTemplate -> {
                        var allowedUrl = allowedUrlTemplate.formatted(entry.getId());
                        return url.toExternalForm().startsWith(allowedUrl);
                    })
            ) {
                throw new IdentifierValidationFailedException(
                    "DID points to an unknown base registry. Your data: '%s' Expected prefix: '%s'".formatted(
                        url,
                        identifierRegistryProperties.defaultPublicResolveUrlTemplate().formatted(entry.getId())
                    ),
                    null
                );
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IdentifierValidationFailedException("DID points to malformed URL", e);
        } catch (DidResolveException e) {
            throw new IdentifierValidationFailedException("DID malformed", e);
        }
    }

    private void validateDidIsOnlyControlledByItself(JsonNode didDoc) {
        var did = didDoc.get("id").asText();

        for (var controllerField : didDoc.findValues("controller")) {
            String controllerId;
            if (controllerField.isArray()) {
                ArrayNode controllerArray = (ArrayNode) controllerField;
                if (controllerArray.size() != 1) {
                    throw new IdentifierValidationFailedException(
                        "Field %s needs to have exactly one entry, which needs to be of the same value as DID.".formatted(
                            controllerField
                        ),
                        null
                    );
                }
                controllerId = controllerArray.get(0).asText();
            } else {
                controllerId = controllerField.asText();
            }

            if (did.compareTo(controllerId) != 0) throw new IdentifierValidationFailedException(
                "All `controller` fields need to be of the same value as DID.",
                null
            );
        }
    }
}
