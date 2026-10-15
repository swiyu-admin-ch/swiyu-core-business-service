package ch.admin.bj.swiyu.core.business.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentConfig;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.config.SaferpayConfig;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.DefaultSaferpayClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * A stage that does not offer payments yet has none of the payment values set (all environment variables
 * resolve to the empty default of application.yml) and uses the real Saferpay client. The application
 * must still start. Only the payment configuration is loaded here, not the whole application.
 */
class PaymentNotConfiguredStartupTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withConfiguration(
            AutoConfigurations.of(
                SslAutoConfiguration.class,
                HttpClientAutoConfiguration.class,
                ImperativeHttpClientAutoConfiguration.class,
                RestClientAutoConfiguration.class
            )
        )
        .withUserConfiguration(PaymentTestConfiguration.class)
        .withPropertyValues("saferpay.mock=false");

    @Test
    void paymentDisabledAndNothingConfigured_contextStarts() {
        contextRunner
            .withPropertyValues("features.EIDARTFE_1297_PAYMENT=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(SaferpayClient.class)).isInstanceOf(DefaultSaferpayClient.class);
                assertThat(context.getBean(PaymentProperties.class).allowedReturnOrigins()).isEmpty();
            });
    }

    @Test
    void paymentEnabledAndNothingConfigured_contextFailsNamingTheMissingProperties() {
        contextRunner
            .withPropertyValues("features.EIDARTFE_1297_PAYMENT=true")
            .run(context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("payment.saferpay.base-url")
                    .hasMessageContaining("payment.allowed-return-origins")
            );
    }

    @Configuration
    @EnableConfigurationProperties({ PaymentProperties.class, FeaturesProperties.class })
    @Import({ PaymentConfig.class, SaferpayConfig.class, DefaultSaferpayClient.class })
    static class PaymentTestConfiguration {}
}
