package ch.admin.bj.swiyu.core.business.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentMapperTest {

    @Test
    void toProductType_and_toProductTypeDto_roundTrip() {
        assertThat(PaymentMapper.toProductType(ProductTypeDto.BASE_REGISTRATION)).isEqualTo(
            ProductType.BASE_REGISTRATION
        );
        assertThat(PaymentMapper.toProductTypeDto(ProductType.BASE_REGISTRATION)).isEqualTo(
            ProductTypeDto.BASE_REGISTRATION
        );
    }

    @Test
    void toResponseDto_mapsIdBusinessPartnerIdAndRedirectUrl() {
        var businessPartnerId = UUID.randomUUID();
        var payment = new Payment(
            UUID.randomUUID(),
            businessPartnerId,
            "P-AAAAAAAA",
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
        payment.applySaferpayInitialization(
            "token",
            Instant.now().plusSeconds(600),
            "https://saferpay.example/redirect/abc"
        );

        var dto = PaymentMapper.toResponseDto(payment);

        assertThat(dto.paymentId()).isEqualTo(payment.getId());
        assertThat(dto.businessPartnerId()).isEqualTo(businessPartnerId);
        assertThat(dto.redirectUrl()).hasToString("https://saferpay.example/redirect/abc");
    }
}
