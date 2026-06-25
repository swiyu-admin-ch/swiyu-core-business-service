package ch.admin.bj.swiyu.core.business.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final BuildProperties buildProperties;
    private static final String API_PACKAGE_NAME = "ch.admin.bj.swiyu.core.business";

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("SWIYU Service API")
                    .description("APIs for the SWIYU Services")
                    .version(buildProperties.getVersion())
            )
            .addSecurityItem(new SecurityRequirement().addList("OIDC").addList("bearer-jwt"))
            .components(
                new Components().addSecuritySchemes(
                    "bearer-jwt",
                    new SecurityScheme().bearerFormat("jwt").type(SecurityScheme.Type.HTTP).scheme("bearer")
                )
            );
    }

    @Bean
    public GroupedOpenApi internal() {
        return GroupedOpenApi.builder()
            .group("INTERNAL")
            .pathsToMatch("/api/*/internal/**", "/api/*/management/**")
            .packagesToScan(API_PACKAGE_NAME)
            .addOpenApiCustomizer(openApi ->
                openApi.info(
                    new Info()
                        .title("Internal API")
                        .description("IF-009 - Internal API")
                        .version(buildProperties.getVersion())
                )
            )
            .build();
    }

    @Bean
    public GroupedOpenApi identifier() {
        return GroupedOpenApi.builder()
            .group("B2B_IDENTIFIER")
            .pathsToMatch("/api/*/identifier/**")
            .packagesToScan(API_PACKAGE_NAME)
            .addOpenApiCustomizer(openApi ->
                openApi.info(
                    new Info()
                        .title("Business Identifier API")
                        .description("IF-010 - B2B Identifier API.")
                        .version(buildProperties.getVersion())
                )
            )
            .build();
    }

    @Bean
    public GroupedOpenApi status() {
        return GroupedOpenApi.builder()
            .group("B2B_STATUS")
            .pathsToMatch("/api/*/status/**")
            .packagesToScan(API_PACKAGE_NAME)
            .addOpenApiCustomizer(openApi ->
                openApi.info(
                    new Info()
                        .title("Business Status API")
                        .description("IF-011 - B2B Status API.")
                        .version(buildProperties.getVersion())
                )
            )
            .build();
    }

    @Bean
    public GroupedOpenApi trust() {
        return GroupedOpenApi.builder()
            .group("B2B_TRUST")
            .pathsToMatch("/api/*/trust/**")
            .packagesToScan(API_PACKAGE_NAME)
            .addOpenApiCustomizer(openApi ->
                openApi.info(
                    new Info()
                        .title("Business Trust API")
                        .description("IF-014 - B2B Trust API.")
                        .version(buildProperties.getVersion())
                )
            )
            .build();
    }
}
