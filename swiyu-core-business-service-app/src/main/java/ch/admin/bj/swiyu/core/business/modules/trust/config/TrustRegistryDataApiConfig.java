package ch.admin.bj.swiyu.core.business.modules.trust.config;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import ch.admin.bj.swiyu.trust.registry.client.api.NonCompliantActorApi;
import ch.admin.bj.swiyu.trust.registry.client.api.TrustStatementApi;
import ch.admin.bj.swiyu.trust.registry.client.api.VcSchemaApi;
import ch.admin.bj.swiyu.trust.registry.client.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TrustRegistryDataApiConfig {

    private final TrustRegistryProperties properties;

    @Bean
    public ApiClient trustRegistryApiClient(JeapOAuth2RestClientBuilderFactory factory) {
        var restClient = factory.createForTokenFromIncomingRequest().build();
        return new ApiClient(restClient).setBasePath(String.valueOf(properties.dataServiceBaseUrl()));
    }

    @Bean
    public NonCompliantActorApi nonCompliantActorApi(ApiClient trustRegistryApiClient) {
        return new NonCompliantActorApi(trustRegistryApiClient);
    }

    @Bean
    public TrustStatementApi trustStatementApi(ApiClient trustRegistryApiClient) {
        return new TrustStatementApi(trustRegistryApiClient);
    }

    @Bean
    public VcSchemaApi vcSchemaApi(ApiClient trustRegistryApiClient) {
        return new VcSchemaApi(trustRegistryApiClient);
    }
}
