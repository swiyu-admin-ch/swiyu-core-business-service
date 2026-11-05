package ch.admin.bj.swiyu.core.business.modules.payment.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayAssertResult;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayCaptureResult;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializeResult;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayAmount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayBrand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayLiability;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayPaymentMeans;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayTransaction;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayTransactionReference;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentFinalizationFailedException;
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
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    TransactionTemplate transactionTemplate;

    @MockitoBean
    SaferpayClient saferpayClient;

    @MockitoBean
    TransactionalOutbox transactionalOutbox;

    private UUID testBusinessPartnerId;

    @BeforeEach
    void setUp() {
        // Clean up any leftover data - truncateTables handles FK order correctly
        // When not in @Transactional, TestTransaction.isActive() returns false, so it just does the deletes
        repos.truncateTables();

        // Create a unique business partner for this test to avoid unique constraint conflicts
        testBusinessPartnerId = UUID.randomUUID();
        var testPartner = businessPartnerA();
        testPartner.setId(testBusinessPartnerId);
        repos.businessPartner.saveAndFlush(testPartner);
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
                testBusinessPartnerId,
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
            testBusinessPartnerId,
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
                testBusinessPartnerId,
                "P-00000001",
                ProductType.BASE_REGISTRATION,
                Amount.chf(15_000),
                "https://portal.example.local/callback"
            )
        );
        var second = new Payment(
            UUID.randomUUID(),
            testBusinessPartnerId,
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

    // ===== finalizePayment tests =====

    @Test
    void finalizePayment_successfulAssertAndCapture_marksPaymentSucceeded() {
        // Given: a payment with Saferpay token
        var payment = createPendingPaymentWithToken("token-123");
        var transactionId = "txn-456";

        // Mock Saferpay Assert response with AUTHORIZED status and correct amount/currency
        var assertResult = createAssertResult(transactionId, "AUTHORIZED", 15_000L, "CHF", true, "VISA");
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        // Mock Saferpay Capture response with CAPTURED status
        var captureResult = createCaptureResult(transactionId, "CAPTURED");
        when(saferpayClient.captureTransaction(any())).thenReturn(captureResult);

        // When
        paymentService.finalizePayment(payment.getId());

        // Then: payment should be SUCCEEDED with all receipt data
        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(updated.getSaferpayTransactionId()).isEqualTo(transactionId);
        assertThat(updated.getPayedAt()).isNotNull();
        assertThat(updated.getPaymentMethod()).isEqualTo("VISA"); // from test data
        assertThat(updated.getHoldername()).isEqualTo("Cardholder");
        assertThat(updated.getLiabilityShift()).isTrue();
    }

    @Test
    void finalizePayment_assertFails_marksPaymentFailedAndThrows() {
        // Given: a payment with Saferpay token
        var payment = createPendingPaymentWithToken("token-123");

        // Mock Saferpay Assert to throw
        when(saferpayClient.assertPayment(any())).thenThrow(new IllegalStateException("saferpay down"));

        // When / Then
        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("Saferpay Assert failed");

        // Payment should be FAILED (persisted in separate transaction)
        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_assertReturnsNoTransaction_marksPaymentFailedAndThrows() {
        var payment = createPendingPaymentWithToken("token-123");
        var assertResult = createAssertResultNullTransaction();
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("no transaction");

        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_assertReturnsTransactionWithoutAmount_callsCancelAndMarksFailed() {
        var payment = createPendingPaymentWithToken("token-123");
        // Transaction with null amount
        var transaction = new SaferpayTransaction(
            "txn-123",
            "AUTHORIZED",
            "PAYMENT",
            null, // null amount
            "CHF",
            Instant.now().toString(),
            "order-123",
            "Acquirer",
            new SaferpayPaymentMeans(new SaferpayBrand("VISA")),
            new SaferpayTransactionReference("txn-123")
        );
        var liability = new SaferpayLiability(true);
        var assertResult = new SaferpayAssertResult(transaction, liability);
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("missing amount or currency");

        // Verify cancelTransaction was called to release authorization
        verify(saferpayClient).cancelTransaction(any());

        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_assertReturnsTransactionWithoutCurrency_callsCancelAndMarksFailed() {
        var payment = createPendingPaymentWithToken("token-123");
        // Transaction with null currencyCode
        var transaction = new SaferpayTransaction(
            "txn-123",
            "AUTHORIZED",
            "PAYMENT",
            new SaferpayAmount(15_000L, "CHF"),
            null, // null currencyCode
            Instant.now().toString(),
            "order-123",
            "Acquirer",
            new SaferpayPaymentMeans(new SaferpayBrand("VISA")),
            new SaferpayTransactionReference("txn-123")
        );
        var liability = new SaferpayLiability(true);
        var assertResult = new SaferpayAssertResult(transaction, liability);
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("missing amount or currency");

        // Verify cancelTransaction was called to release authorization
        verify(saferpayClient).cancelTransaction(any());

        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_statusNotAuthorized_marksPaymentFailedAndReturns() {
        var payment = createPendingPaymentWithToken("token-123");
        var assertResult = createAssertResult("txn-123", "CANCELLED", 15_000L, "CHF", true, "VISA");
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        // When - should NOT throw, just return
        paymentService.finalizePayment(payment.getId());

        // Then: payment should be FAILED
        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_amountMismatch_callsCancelAndMarksFailed() {
        var payment = createPendingPaymentWithToken("token-123");
        // Expected: 15000 (CHF 150.00), Actual: 20000 (mismatch)
        var assertResult = createAssertResult("txn-123", "AUTHORIZED", 20_000L, "CHF", true, "VISA");
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("Amount mismatch");

        // Verify cancelTransaction was called to release authorization
        verify(saferpayClient).cancelTransaction(any());

        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_currencyMismatch_callsCancelAndMarksFailed() {
        var payment = createPendingPaymentWithToken("token-123");
        // Expected: CHF, Actual: EUR (mismatch)
        var assertResult = createAssertResult("txn-123", "AUTHORIZED", 15_000L, "EUR", true, "VISA");
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("Currency mismatch");

        // Verify cancelTransaction was called to release authorization
        verify(saferpayClient).cancelTransaction(any());

        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_liabilityShiftFalse_with3DSMethod_callsCancelAndMarksFailed() {
        var payment = createPendingPaymentWithToken("token-123");
        // liabilityShift = false, paymentMethod = VISA (requires 3DS)
        var assertResult = createAssertResult("txn-123", "AUTHORIZED", 15_000L, "CHF", false, "VISA");
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("Liability shift not achieved");

        // Verify cancelTransaction was called
        verify(saferpayClient).cancelTransaction(any());

        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_liabilityShiftFalse_without3DSMethod_doesNotCallCancelAndMarksFailed() {
        var payment = createPendingPaymentWithToken("token-123");
        // liabilityShift = false, paymentMethod = TWINT (does not require 3DS)
        var assertResult = createAssertResult("txn-123", "AUTHORIZED", 15_000L, "CHF", false, "TWINT");
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("Liability shift not achieved");

        // Verify cancelTransaction was NOT called for non-3DS method
        verify(saferpayClient, never()).cancelTransaction(any());

        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_captureFails_throwsButDoesNotMarkFailed() {
        var payment = createPendingPaymentWithToken("token-123");
        var assertResult = createAssertResult("txn-123", "AUTHORIZED", 15_000L, "CHF", true, "VISA");
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        // Capture throws
        when(saferpayClient.captureTransaction(any())).thenThrow(new IllegalStateException("capture failed"));

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("Capture failed");

        // Payment should NOT be marked FAILED (capture can be retried)
        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(updated.getSaferpayTransactionId()).isEqualTo("txn-123"); // transactionId was set
    }

    @Test
    void finalizePayment_captureReturnsNotCaptured_throws() {
        var payment = createPendingPaymentWithToken("token-123");
        var assertResult = createAssertResult("txn-123", "AUTHORIZED", 15_000L, "CHF", true, "VISA");
        when(saferpayClient.assertPayment(any())).thenReturn(assertResult);

        // Capture returns non-CAPTURED status
        var captureResult = createCaptureResult("txn-123", "AUTHORIZED"); // still AUTHORIZED, not CAPTURED
        when(saferpayClient.captureTransaction(any())).thenReturn(captureResult);

        assertThatThrownBy(() -> paymentService.finalizePayment(payment.getId()))
            .isInstanceOf(PaymentFinalizationFailedException.class)
            .hasMessageContaining("capture status is AUTHORIZED, expected CAPTURED");

        var updated = repos.payment.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void finalizePayment_alreadySucceeded_skipsFinalization() {
        var payment = createSucceededPayment();
        var paymentId = payment.getId();

        paymentService.finalizePayment(paymentId);

        // Should not call Saferpay at all
        assertThat(repos.payment.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void finalizePayment_alreadyFailed_skipsFinalization() {
        var payment = createFailedPayment();
        var paymentId = payment.getId();

        paymentService.finalizePayment(paymentId);

        // Should not call Saferpay at all
        assertThat(repos.payment.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void finalizePayment_alreadyCancelled_skipsFinalization() {
        var payment = createCancelledPayment();
        var paymentId = payment.getId();

        paymentService.finalizePayment(paymentId);

        // Should not call Saferpay at all
        assertThat(repos.payment.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    // Helper methods

    private Payment createPendingPaymentWithToken(String token) {
        var payment = new Payment(
            UUID.randomUUID(),
            testBusinessPartnerId,
            "P-IT-" + UUID.randomUUID().toString().substring(0, 8),
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
        payment.applySaferpayInitialization(token, Instant.now().plusSeconds(600), "https://saferpay.example/redirect");
        return repos.payment.saveAndFlush(payment);
    }

    private Payment createSucceededPayment() {
        var payment = new Payment(
            UUID.randomUUID(),
            testBusinessPartnerId,
            "P-IT-SUCCEEDED",
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
        payment.applySaferpayInitialization(
            "token",
            Instant.now().plusSeconds(600),
            "https://saferpay.example/redirect"
        );
        payment.markAsSucceeded("txn-123", Instant.now(), "VISA", "Cardholder", true);
        return repos.payment.saveAndFlush(payment);
    }

    private Payment createFailedPayment() {
        var payment = new Payment(
            UUID.randomUUID(),
            testBusinessPartnerId,
            "P-IT-FAILED",
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
        payment.markAsFailed();
        return repos.payment.saveAndFlush(payment);
    }

    private Payment createCancelledPayment() {
        var payment = new Payment(
            UUID.randomUUID(),
            testBusinessPartnerId,
            "P-IT-CANCELLED",
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
        payment.markAsCancelled();
        return repos.payment.saveAndFlush(payment);
    }

    private SaferpayAssertResult createAssertResult(
        String transactionId,
        String status,
        long amountValue,
        String currencyCode,
        boolean liabilityShift
    ) {
        return createAssertResult(transactionId, status, amountValue, currencyCode, liabilityShift, "VISA");
    }

    private SaferpayAssertResult createAssertResult(
        String transactionId,
        String status,
        long amountValue,
        String currencyCode,
        boolean liabilityShift,
        String paymentMethod
    ) {
        var transaction = new SaferpayTransaction(
            transactionId,
            status,
            "PAYMENT",
            new SaferpayAmount(amountValue, currencyCode),
            currencyCode,
            Instant.now().toString(),
            "order-123",
            "Acquirer",
            new SaferpayPaymentMeans(new SaferpayBrand(paymentMethod)),
            new SaferpayTransactionReference(transactionId)
        );
        var liability = new SaferpayLiability(liabilityShift);
        return new SaferpayAssertResult(transaction, liability);
    }

    private SaferpayAssertResult createAssertResultNullTransaction() {
        return new SaferpayAssertResult(null, null);
    }

    private SaferpayCaptureResult createCaptureResult(String transactionId, String status) {
        var transaction = new SaferpayTransaction(
            transactionId,
            status,
            "PAYMENT",
            new SaferpayAmount(15_000L, "CHF"),
            "CHF",
            Instant.now().toString(),
            "order-123",
            "Acquirer",
            new SaferpayPaymentMeans(new SaferpayBrand("VISA")),
            new SaferpayTransactionReference(transactionId)
        );
        return new SaferpayCaptureResult(transaction);
    }
}
