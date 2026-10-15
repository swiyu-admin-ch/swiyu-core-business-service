package ch.admin.bj.swiyu.core.business.modules.payment.config;

import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Checks the payment configuration at startup, but only when the payment feature is enabled. This way a
 * stage that does not offer payments yet can start without any payment values, while a stage with the
 * feature enabled still fails fast on a missing value.
 */
@Configuration
@RequiredArgsConstructor
public class PaymentConfig {

    private final FeaturesProperties featuresProperties;
    private final PaymentProperties paymentProperties;

    @PostConstruct
    public void validateWhenPaymentEnabled() {
        if (!Boolean.TRUE.equals(featuresProperties.getEidartfe1297Payment())) {
            return;
        }
        var missing = new ArrayList<String>();
        if (paymentProperties.allowedReturnOrigins().isEmpty()) {
            missing.add("payment.allowed-return-origins");
        }
        if (!StringUtils.hasText(paymentProperties.saferpay().terminalId())) {
            missing.add("payment.saferpay.terminal-id");
        }
        if (!StringUtils.hasText(paymentProperties.saferpay().customerId())) {
            missing.add("payment.saferpay.customer-id");
        }
        var baseUrl = paymentProperties.saferpay().baseUrl();
        if (baseUrl == null || !StringUtils.hasText(baseUrl.toString())) {
            missing.add("payment.saferpay.base-url");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "The payment feature is enabled, but these properties are not set: " + String.join(", ", missing)
            );
        }
    }
}
