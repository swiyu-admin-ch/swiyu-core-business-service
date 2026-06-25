package ch.admin.bj.swiyu.core.business.common.antivirus;

import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
class AntivirusConfig {

    private final AntivirusProperties antivirusProperties;

    @Bean
    public ApiClient antivirusApiClient(RestClientBuilderConfigurer factory) {
        var restClient = factory.configure(RestClient.builder()).build();

        var client = new ApiClient(restClient).setBasePath(antivirusProperties.getBaseUrl());
        client.setUsername(antivirusProperties.username());
        client.setPassword(antivirusProperties.password());
        return client;
    }

    @Bean
    public ScanApi antivirusScanApi(ApiClient antivirusApiClient) {
        return new ScanApi(antivirusApiClient);
    }
}
