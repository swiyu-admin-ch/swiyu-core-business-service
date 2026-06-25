package ch.admin.bj.swiyu.core.business.modules.identifier.config;

import ch.admin.bj.swiyu.core.business.common.utils.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@RequiredArgsConstructor
public class IdentifierSchemaConfig {

    public final ObjectMapper objectMapper;

    @Value("classpath:schema/didtdw.schema.json")
    private Resource didtdwSchema;

    @Value("classpath:schema/didwebvh.schema.json")
    private Resource didwebvhSchema;

    @Value("classpath:schema/diddoc.schema.json")
    private Resource diddocSchema;

    @Bean
    public JsonSchema didTdwSchema(JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig jsonSchemaConfig)
        throws IOException {
        return jsonSchemaFactory.getSchema(objectMapper.readTree(FileUtil.asString(didtdwSchema)), jsonSchemaConfig);
    }

    @Bean
    public JsonSchema didWebvhSchema(JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig jsonSchemaConfig)
        throws IOException {
        return jsonSchemaFactory.getSchema(objectMapper.readTree(FileUtil.asString(didwebvhSchema)), jsonSchemaConfig);
    }

    @Bean
    public JsonSchema didDocSchema(JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig jsonSchemaConfig)
        throws IOException {
        return jsonSchemaFactory.getSchema(objectMapper.readTree(FileUtil.asString(diddocSchema)), jsonSchemaConfig);
    }
}
