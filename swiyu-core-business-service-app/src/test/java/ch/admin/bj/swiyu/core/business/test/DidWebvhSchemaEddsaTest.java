package ch.admin.bj.swiyu.core.business.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the did:webvh log entry schema accepts EdDSA (Ed25519/OKP) verification methods
 * in addition to the EC P-256 ones, while still rejecting unsupported key types.
 */
class DidWebvhSchemaEddsaTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static JsonSchema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (
            var schemaInputStream = DidWebvhSchemaEddsaTest.class.getClassLoader().getResourceAsStream(
                "schema/didwebvh.schema.json"
            )
        ) {
            var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            var schemaNode = mapper.readTree(schemaInputStream);
            schema = factory.getSchema(schemaNode);
        }
    }

    @Test
    void validEddsaDidWebvhLogEntry_isAccepted() throws IOException {
        var document = loadLogEntry("data/identifier/didtoolbox/2.3.0/valid_eddsa.jsonl");
        var errors = schema.validate(document);
        assertTrue(errors.isEmpty(), "EdDSA log entry should be valid, but was: " + errors);
    }

    @Test
    void validEcP256DidWebvhLogEntry_isStillAccepted() throws IOException {
        var document = loadLogEntry("data/identifier/didtoolbox/2.1.0/valid.jsonl");
        var errors = schema.validate(document);
        assertTrue(errors.isEmpty(), "EC P-256 log entry should still be valid, but was: " + errors);
    }

    @Test
    void unsupportedKeyType_isRejected() throws IOException {
        var document = loadLogEntry("data/identifier/didtoolbox/2.3.0/valid_eddsa.jsonl");

        // Replace the first verification method's OKP/Ed25519 key with an unsupported RSA JWK.
        JsonNode verificationMethod = document.at("/state/verificationMethod/0");
        ObjectNode publicKeyJwk = (ObjectNode) verificationMethod.get("publicKeyJwk");
        publicKeyJwk.put("kty", "RSA");
        publicKeyJwk.put("crv", "not-a-curve");
        publicKeyJwk.put("e", "AQAB");
        publicKeyJwk.put("n", "unused");

        var errors = schema.validate(document);
        assertFalse(errors.isEmpty(), "Log entry with unsupported RSA key should be invalid");
    }

    private static JsonNode loadLogEntry(String path) throws IOException {
        try (var inputStream = DidWebvhSchemaEddsaTest.class.getClassLoader().getResourceAsStream(path)) {
            return mapper.readTree(inputStream);
        }
    }
}
