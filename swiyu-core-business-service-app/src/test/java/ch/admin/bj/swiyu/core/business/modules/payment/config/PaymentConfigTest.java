package ch.admin.bj.swiyu.core.business.modules.payment.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties.SaferpayProperties;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentConfigTest {

    private static final SaferpayProperties COMPLETE = new SaferpayProperties(
        "terminal-1",
        "customer-1",
        URI.create("https://test.saferpay.com/api"),
        null
    );
    private static final SaferpayProperties EMPTY = new SaferpayProperties("", "", null, null);

    @Test
    void validate_featureDisabledAndNothingConfigured_doesNotThrow() {
        assertThatCode(() -> config(false, null, EMPTY).validateWhenPaymentEnabled()).doesNotThrowAnyException();
    }

    @Test
    void validate_featureEnabledAndEverythingConfigured_doesNotThrow() {
        assertThatCode(() ->
            config(true, List.of("https://portal.example.local"), COMPLETE).validateWhenPaymentEnabled()
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_featureEnabledAndNothingConfigured_throwsNamingAllMissingProperties() {
        var config = config(true, null, EMPTY);

        assertThatThrownBy(config::validateWhenPaymentEnabled)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("payment.allowed-return-origins")
            .hasMessageContaining("payment.saferpay.terminal-id")
            .hasMessageContaining("payment.saferpay.customer-id")
            .hasMessageContaining("payment.saferpay.base-url");
    }

    private PaymentConfig config(
        boolean paymentEnabled,
        List<String> allowedReturnOrigins,
        SaferpayProperties saferpay
    ) {
        var features = new FeaturesProperties();
        features.setEidartfe1297Payment(paymentEnabled);
        var properties = new PaymentProperties(
            allowedReturnOrigins,
            saferpay,
            List.of(new PaymentProperties.Product("BASE_REGISTRATION", "Base", 15_000))
        );
        return new PaymentConfig(features, properties);
    }
}
