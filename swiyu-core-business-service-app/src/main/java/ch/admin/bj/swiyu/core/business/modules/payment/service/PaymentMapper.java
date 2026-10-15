package ch.admin.bj.swiyu.core.business.modules.payment.service;

import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationResponseDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PaymentMapper {

    public static PaymentInitializationResponseDto toResponseDto(Payment payment) {
        return PaymentInitializationResponseDto.builder()
            .paymentId(payment.getId())
            .businessPartnerId(payment.getBusinessPartnerId())
            .redirectUrl(toUrl(payment.getSaferpayRedirectUrl()))
            .build();
    }

    public static ProductType toProductType(ProductTypeDto dto) {
        return switch (dto) {
            case BASE_REGISTRATION -> ProductType.BASE_REGISTRATION;
        };
    }

    public static ProductTypeDto toProductTypeDto(ProductType type) {
        return switch (type) {
            case BASE_REGISTRATION -> ProductTypeDto.BASE_REGISTRATION;
        };
    }

    private static URL toUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Saferpay returned an invalid redirect URL: " + url, e);
        }
    }
}
