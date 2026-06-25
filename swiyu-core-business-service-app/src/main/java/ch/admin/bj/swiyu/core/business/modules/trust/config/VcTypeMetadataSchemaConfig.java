package ch.admin.bj.swiyu.core.business.modules.trust.config;

import ch.admin.bj.swiyu.core.business.common.utils.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Config to validate the schema for the vc type metadata
 */
@Configuration
@Getter
@RequiredArgsConstructor
public class VcTypeMetadataSchemaConfig {

    private final ObjectMapper objectMapper;

    @Value("classpath:schema/vc-type-metadata.schema.json")
    private Resource vcTypeMetadataSchema;

    @Bean
    public JsonSchema vcTypeMetadataSchema(JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig jsonSchemaConfig)
        throws IOException {
        return jsonSchemaFactory.getSchema(
            objectMapper.readTree(FileUtil.asString(vcTypeMetadataSchema)),
            jsonSchemaConfig
        );
    }
}
