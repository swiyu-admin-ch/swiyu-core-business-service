package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import ch.admin.eportal.pams.client.api.BusinessPartnerApi;
import ch.admin.eportal.pams.client.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PamsConfig {

    @Bean
    @ConditionalOnProperty(name = "pams.mock", havingValue = "false", matchIfMissing = true)
    public PamsClient pamsClient(
        RestClient.Builder restClientBuilder,
        PamsProperties pamsProperties,
        PamsHealthApiClient pamsHealthApiClient
    ) {
        var apiClient = new ApiClient(restClientBuilder.build());
        apiClient.setBasePath(pamsProperties.getApiUrl());
        var businessPartnerApi = new BusinessPartnerApi(apiClient);
        return new DefaultPamsClient(businessPartnerApi, pamsProperties, pamsHealthApiClient);
    }

    @ConditionalOnProperty(name = "pams.mock", havingValue = "true")
    @Bean
    public PamsClient mockPamsClient() {
        return new MockPamsClient();
    }
}
