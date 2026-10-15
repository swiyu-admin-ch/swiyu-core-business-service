package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * The products that can be paid, as configured in payment.product-catalog. The configured product types
 * are mapped to ProductType at startup, so an unknown type stops the application from starting.
 */
@Service
public class PaymentProductCatalog {

    private final Map<ProductType, PaymentProperties.Product> products = new EnumMap<>(ProductType.class);

    public PaymentProductCatalog(PaymentProperties paymentProperties) {
        for (var product : paymentProperties.productCatalog()) {
            products.put(toProductType(product.type()), product);
        }
    }

    /** Price of the product in Rappen. */
    public long priceFor(ProductType type) {
        return productFor(type).price();
    }

    public String descriptionFor(ProductType type) {
        return productFor(type).description();
    }

    private PaymentProperties.Product productFor(ProductType type) {
        var product = products.get(type);
        if (product == null) {
            throw new IllegalStateException("No price configured for product type " + type);
        }
        return product;
    }

    private static ProductType toProductType(String type) {
        try {
            return ProductType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown product type '%s' in payment.product-catalog".formatted(type), e);
        }
    }
}
