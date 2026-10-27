package ch.admin.bj.swiyu.core.business.modules.payment.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.insertTestBusinessPartners;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializeResult;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the two-phase-transaction design: the PENDING payment is fully committed (visible to a
 * fresh repository read) before the Saferpay call happens, and the row is updated afterward in a
 * second transaction.
 */
@ActiveProfiles("test")
@SpringBootTest
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken
class PaymentServiceIT {

    @Autowired
    PaymentService paymentService;

    @Autowired
    TestRepositories repos;

    @MockitoBean
    SaferpayClient saferpayClient;

    @BeforeEach
    void setUp() {
        insertTestBusinessPartners(repos.businessPartner);
        repos.payment.deleteAll();
    }

    @Test
    void initiatePayment_commitsPendingPaymentBeforeCallingSaferpay_thenPersistsSaferpayResult() throws Exception {
        when(saferpayClient.initialize(any())).thenAnswer(invocation -> {
            // At this point, the first transaction (creating the PENDING payment) must already be
            // committed - visible via a fresh repository read, before the Saferpay call happens.
            var pendingPayments = repos.payment.findAll();
            assertThat(pendingPayments).hasSize(1);
            assertThat(pendingPayments.getFirst().getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(pendingPayments.getFirst().getSaferpayToken()).isNull();

            return new SaferpayInitializeResult(
                "token-xyz",
                "https://saferpay.example/redirect/xyz",
                Instant.now().plusSeconds(600)
            );
        });

        var response = paymentService.initiatePayment(
            new PaymentInitializationRequestDto(
                ENTITY_A,
                ProductTypeDto.BASE_REGISTRATION,
                URI.create("https://portal.example.local/callback").toURL()
            )
        );

        var updated = repos.payment.findById(response.paymentId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(updated.getSaferpayToken()).isEqualTo("token-xyz");
        assertThat(updated.getSaferpayRedirectUrl()).isEqualTo("https://saferpay.example/redirect/xyz");
        assertThat(updated.getSaferpayExpiration()).isNotNull();
        assertThat(updated.getReturnUrl()).isEqualTo("https://portal.example.local/callback/" + response.paymentId());
    }

    @Test
    void initiatePayment_saferpayFails_marksPaymentFailedSoANewPaymentCanBeInitiated() throws Exception {
        var request = new PaymentInitializationRequestDto(
            ENTITY_A,
            ProductTypeDto.BASE_REGISTRATION,
            URI.create("https://portal.example.local/callback").toURL()
        );
        when(saferpayClient.initialize(any()))
            .thenThrow(new IllegalStateException("saferpay down"))
            .thenReturn(new SaferpayInitializeResult("token-2", "https://saferpay.example/redirect/2", Instant.now()));

        assertThatThrownBy(() -> paymentService.initiatePayment(request)).isInstanceOf(IllegalStateException.class);
        assertThat(repos.payment.findAll()).extracting(Payment::getStatus).containsExactly(PaymentStatus.FAILED);

        paymentService.initiatePayment(request);

        assertThat(repos.payment.findAll())
            .extracting(Payment::getStatus)
            .containsExactlyInAnyOrder(PaymentStatus.FAILED, PaymentStatus.PENDING);
    }

    @Test
    void uniqueIndex_rejectsSecondPendingPaymentForSameBusinessPartnerAndProduct() {
        repos.payment.saveAndFlush(
            new Payment(
                UUID.randomUUID(),
                ENTITY_A,
                "P-00000001",
                ProductType.BASE_REGISTRATION,
                Amount.chf(15_000),
                "https://portal.example.local/callback"
            )
        );
        var second = new Payment(
            UUID.randomUUID(),
            ENTITY_A,
            "P-00000002",
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );

        // the index name in the message is what PaymentService relies on to detect this case
        assertThatThrownBy(() -> repos.payment.saveAndFlush(second))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasStackTraceContaining("payment_business_partner_id_product_pending_key");
    }
}
