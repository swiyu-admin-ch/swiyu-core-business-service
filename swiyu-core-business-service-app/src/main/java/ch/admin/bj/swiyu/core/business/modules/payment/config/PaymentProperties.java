package ch.admin.bj.swiyu.core.business.modules.payment.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The allowed return origins and the Saferpay terminal id, customer id and base url are not validated
 * here: the application must be able to start without them as long as the payment feature is disabled.
 * {@link PaymentConfig} checks them at startup when the feature is enabled.
 */
@Validated
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
    List<String> allowedReturnOrigins,
    @Valid @NotNull SaferpayProperties saferpay,
    @Valid @NotEmpty List<Product> productCatalog
) {
    public PaymentProperties {
        allowedReturnOrigins = allowedReturnOrigins == null ? List.of() : allowedReturnOrigins;
    }

    /**
     * Connection to our Saferpay account.
     *
     * @param terminalId id of the Saferpay terminal the payments are booked on. A terminal belongs to a
     *                   customer and defines e.g. the accepted payment methods.
     * @param customerId id of our Saferpay account (customer), sent in the header of every request
     * @param baseUrl    root url of the Saferpay JSON API, differs between Saferpay TEST and PROD
     * @param configSet  name of the Payment Page configuration in the Saferpay Backoffice. It defines the
     *                   look (branding) of the payment page. Optional: when empty, Saferpay uses the
     *                   configuration marked as default.
     */
    public record SaferpayProperties(String terminalId, String customerId, URI baseUrl, String configSet) {}

    /**
     * A product that can be paid.
     *
     * @param type        name of a ProductType, e.g. BASE_REGISTRATION. A String because config must not
     *                    reference domain types; PaymentProductCatalog checks it at startup.
     * @param description shown to the user on the Saferpay payment page
     * @param price       in Rappen (CHF minor units), e.g. 15000 = CHF 150.00
     */
    @Validated
    public record Product(@NotEmpty String type, @NotEmpty String description, @Min(0) int price) {}
}
