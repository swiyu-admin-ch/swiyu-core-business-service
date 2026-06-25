package ch.admin.bj.swiyu.core.business.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StatuslistSchemaTestData.class)
public class DidTdwSchemaTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static JsonSchema schema;

    @Autowired
    StatuslistSchemaTestData testData;

    @BeforeAll
    public static void loadSchema() throws Exception {
        var schemaStream = DidTdwSchemaTest.class.getClassLoader().getResourceAsStream("schema/didtdw.schema.json");

        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        var schemaNode = mapper.readTree(schemaStream);
        schema = factory.getSchema(schemaNode);
    }

    @Test
    public void testWitnessMustNotBePresent() throws Exception {
        var json = testData.noWitness();

        var document = mapper.readTree(json);
        var errors = schema.validate(document);
        assertFalse(errors.isEmpty(), "Document with 'witness' should be invalid");
    }
}
