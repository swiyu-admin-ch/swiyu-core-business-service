package ch.admin.bj.swiyu.core.business.modules.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.autoconfigure.RestClientSsl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class SaferpayConfig {

    private final PaymentProperties paymentProperties;

    /** Only needed by DefaultSaferpayClient, so not created when the Saferpay mock is used. */
    @Bean
    @ConditionalOnProperty(name = "saferpay.mock", havingValue = "false", matchIfMissing = true)
    public RestClient saferpayRestClient(RestClient.Builder restClientBuilder, RestClientSsl restClientSsl) {
        // fromBundle only adds the client certificate; the timeouts of spring.http.clients stay in effect
        restClientBuilder.apply(restClientSsl.fromBundle("saferpay"));
        // the base url may be missing as long as the payment feature is disabled, see PaymentConfig
        var baseUrl = paymentProperties.saferpay().baseUrl();
        if (baseUrl != null) {
            restClientBuilder.baseUrl(baseUrl.toString());
        }
        return restClientBuilder.build();
    }
}
