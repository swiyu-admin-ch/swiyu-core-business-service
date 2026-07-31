package ch.admin.bj.swiyu.core.business.modules.status.config;

import ch.admin.bj.swiyu.core.business.common.utils.FileUtil;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@Getter
@RequiredArgsConstructor
public class StatusListSchemaConfig {

    @Value("classpath:schema/token-status-list_v0.2.json")
    private Resource tokenStatusListSchema;

    @Bean
    public JsonSchema statusListSchema(JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig jsonSchemaConfig) {
        return jsonSchemaFactory.getSchema(
            FileUtil.asString(tokenStatusListSchema),
            InputFormat.JSON,
            jsonSchemaConfig
        );
    }
}
