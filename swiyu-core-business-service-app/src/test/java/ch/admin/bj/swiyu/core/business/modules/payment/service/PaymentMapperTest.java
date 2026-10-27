package ch.admin.bj.swiyu.core.business.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentStatusDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

    @ParameterizedTest
    @EnumSource(PaymentStatusDto.class)
    void toPaymentStatus_and_toPaymentStatusDto_roundTrip(PaymentStatusDto dto) {
        assertThat(PaymentMapper.toPaymentStatusDto(PaymentMapper.toPaymentStatus(dto))).isEqualTo(dto);
    }

    @Test
    void toPaymentDto_mapsAllFields() {
        var payedAt = Instant.now();
        var payment = new Payment(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "P-AAAAAAAA",
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback/id"
        );
        payment.markAsSucceeded(payedAt);

        var dto = PaymentMapper.toPaymentDto(payment);

        assertThat(dto.id()).isEqualTo(payment.getId());
        assertThat(dto.businessPartnerId()).isEqualTo(payment.getBusinessPartnerId());
        assertThat(dto.referenceId()).isEqualTo("P-AAAAAAAA");
        assertThat(dto.status()).isEqualTo(PaymentStatusDto.SUCCEEDED);
        assertThat(dto.product()).isEqualTo(ProductTypeDto.BASE_REGISTRATION);
        assertThat(dto.returnUrl()).isEqualTo("https://portal.example.local/callback/id");
        assertThat(dto.payedAt()).isEqualTo(payedAt);
        assertThat(dto.amount().value()).isEqualTo(15_000L);
        assertThat(dto.amount().currency()).isEqualTo("CHF");
    }
}
