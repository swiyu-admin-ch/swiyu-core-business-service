package ch.admin.bj.swiyu.core.business.common.config;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JsonSchemaConfig {

    @Bean
    public JsonSchemaFactory jsonSchemaFactory() {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @Bean
    public SchemaValidatorsConfig.Builder jsonSchemaConfigBuilder() {
        return SchemaValidatorsConfig.builder();
    }

    @Bean
    public SchemaValidatorsConfig jsonSchemaValidatorConfig(SchemaValidatorsConfig.Builder jsonSchemaConfigBuilder) {
        return jsonSchemaConfigBuilder.preloadJsonSchema(true).build();
    }

    @Bean
    public JsonMapper schemaValidatorObjectMapper() {
        return JsonMapper.builder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).build();
    }
}
