package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentProductCatalogTest {

    @Test
    void priceAndDescription_comeFromTheConfiguredProduct() {
        var catalog = catalog(new PaymentProperties.Product("BASE_REGISTRATION", "swiyu Base Registry", 15_000));

        assertThat(catalog.priceFor(ProductType.BASE_REGISTRATION)).isEqualTo(15_000);
        assertThat(catalog.descriptionFor(ProductType.BASE_REGISTRATION)).isEqualTo("swiyu Base Registry");
    }

    @Test
    void unknownProductType_failsAtStartup() {
        var unknownProduct = new PaymentProperties.Product("UNKNOWN", "Unknown", 100);

        assertThatThrownBy(() -> catalog(unknownProduct))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("UNKNOWN");
    }

    private static PaymentProductCatalog catalog(PaymentProperties.Product product) {
        return new PaymentProductCatalog(new PaymentProperties(List.of(), null, List.of(product)));
    }
}
