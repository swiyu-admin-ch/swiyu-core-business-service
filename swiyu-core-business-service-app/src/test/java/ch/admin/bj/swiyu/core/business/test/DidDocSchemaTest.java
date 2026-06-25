package ch.admin.bj.swiyu.core.business.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StatuslistSchemaTestData.class)
public class DidDocSchemaTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static JsonSchema schema;

    @Autowired
    StatuslistSchemaTestData testData;

    @BeforeAll
    public static void loadSchema() throws Exception {
        var schemaInputStream = DidDocSchemaTest.class.getClassLoader().getResourceAsStream(
            "schema/diddoc.schema.json"
        );
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        var schemaNode = mapper.readTree(schemaInputStream);
        DidDocSchemaTest.schema = factory.getSchema(schemaNode);
    }

    @Test
    @Disabled("Can be enabled again, when EID-5275 is done")
    public void testControllerMustNotBePresent() throws Exception {
        var json = testData.controller();

        var document = mapper.readTree(json);
        var errors = schema.validate(document);
        assertFalse(errors.isEmpty(), "Document with 'controller' should be invalid");
    }

    @Test
    public void testPublicKeyMultibaseMustNotBePresent() throws Exception {
        var json = testData.pubKeyMulti();

        var document = mapper.readTree(json);
        var errors = schema.validate(document);
        assertFalse(errors.isEmpty(), "Document with 'publicKeyMultibase' should be invalid");
    }
}
