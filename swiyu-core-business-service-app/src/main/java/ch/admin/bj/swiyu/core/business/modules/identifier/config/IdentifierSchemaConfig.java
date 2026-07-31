package ch.admin.bj.swiyu.core.business.modules.identifier.config;

import ch.admin.bj.swiyu.core.business.common.utils.FileUtil;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@RequiredArgsConstructor
public class IdentifierSchemaConfig {

    @Value("classpath:schema/didtdw.schema.json")
    private Resource didtdwSchema;

    @Value("classpath:schema/didwebvh.schema.json")
    private Resource didwebvhSchema;

    @Value("classpath:schema/diddoc.schema.json")
    private Resource diddocSchema;

    @Bean
    public JsonSchema didTdwSchema(JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig jsonSchemaConfig) {
        return jsonSchemaFactory.getSchema(FileUtil.asString(didtdwSchema), InputFormat.JSON, jsonSchemaConfig);
    }

    @Bean
    public JsonSchema didWebvhSchema(JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig jsonSchemaConfig) {
        return jsonSchemaFactory.getSchema(FileUtil.asString(didwebvhSchema), InputFormat.JSON, jsonSchemaConfig);
    }

    @Bean
    public JsonSchema didDocSchema(JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig jsonSchemaConfig) {
        return jsonSchemaFactory.getSchema(FileUtil.asString(diddocSchema), InputFormat.JSON, jsonSchemaConfig);
    }
}
