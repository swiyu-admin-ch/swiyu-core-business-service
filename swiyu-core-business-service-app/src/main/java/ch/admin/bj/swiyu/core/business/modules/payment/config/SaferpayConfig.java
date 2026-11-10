package ch.admin.bj.swiyu.core.business.modules.payment.config;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class SaferpayConfig {

    private static final String SSL_BUNDLE = "saferpay";

    private final PaymentProperties paymentProperties;

    /** Only needed by DefaultSaferpayClient, so not created when the Saferpay mock is used. */
    @Bean
    @ConditionalOnProperty(name = "saferpay.mock", havingValue = "false", matchIfMissing = true)
    public RestClient saferpayRestClient(
        RestClient.Builder restClientBuilder,
        HttpClientSettings httpClientSettings,
        SslBundles sslBundles
    ) {
        var saferpay = paymentProperties.saferpay();
        // Like RestClientSsl.fromBundle: the timeouts of spring.http.clients plus the client certificate of
        // the "saferpay" bundle. Built by hand only to add the proxy, which fromBundle cannot do. Uses
        // HttpComponents explicitly (the proxy setting is Apache specific), so spring.http.clients.imperative.factory
        // and ClientHttpRequestFactoryBuilderCustomizer beans do not apply to this client.
        var settings = httpClientSettings.withSslBundle(sslBundles.getBundle(SSL_BUNDLE));
        restClientBuilder.requestFactory(requestFactoryBuilder(saferpay.proxy()).build(settings));
        // the base url may be missing as long as the payment feature is disabled, see PaymentConfig
        if (saferpay.baseUrl() != null) {
            restClientBuilder.baseUrl(saferpay.baseUrl().toString());
        }
        return restClientBuilder.build();
    }

    /**
     * The HTTP client for Saferpay, sending every request through {@code proxy} if one is configured.
     * Apache HttpClient does not pick up HTTPS_PROXY or the JVM proxy properties on its own, so the
     * proxy is set explicitly, and only on this client: the internal calls of the service stay direct.
     */
    private static HttpComponentsClientHttpRequestFactoryBuilder requestFactoryBuilder(URI proxy) {
        var builder = ClientHttpRequestFactoryBuilder.httpComponents();
        if (proxy == null || proxy.toString().isBlank()) {
            return builder;
        }
        // e.g. "prxp01.admin.ch:8080" without scheme would parse without host and silently go direct
        if (proxy.getHost() == null || proxy.getPort() < 0) {
            throw new IllegalStateException(
                "payment.saferpay.proxy '%s' needs scheme, host and port, e.g. http://prxp01.admin.ch:8080".formatted(
                    proxy
                )
            );
        }
        var proxyHost = new HttpHost(proxy.getScheme(), proxy.getHost(), proxy.getPort());
        return builder.withHttpClientCustomizer(httpClient -> httpClient.setProxy(proxyHost));
    }
}
