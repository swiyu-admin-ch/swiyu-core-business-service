package ch.admin.bj.swiyu.core.business.common;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.config.OpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = { OpenApiConfig.class, OpenApiConfigTest.TestConfig.class })
class OpenApiConfigTest {

    @Configuration
    static class TestConfig {

        @Bean
        public BuildProperties buildProperties() {
            return new BuildProperties(
                new Properties() {
                    {
                        put("version", "test-version");
                    }
                }
            );
        }
    }

    private final OpenApiConfig openApiConfig = new OpenApiConfig(new TestConfig().buildProperties());

    @Test
    void testBaseOpenAPI() {
        OpenAPI openAPI = openApiConfig.api();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearer-jwt");
        assertThat(openAPI.getSecurity()).isNotEmpty();
    }

    @Test
    void testStatusGroup() {
        GroupedOpenApi group = openApiConfig.status();
        assertThat(group.getGroup()).isEqualTo("B2B_STATUS");
    }

    @Test
    void testIdentifierGroup() {
        GroupedOpenApi group = openApiConfig.identifier();
        assertThat(group.getGroup()).isEqualTo("B2B_IDENTIFIER");
    }
}
