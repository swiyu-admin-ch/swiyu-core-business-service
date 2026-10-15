package ch.admin.bj.swiyu.core.business.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentProductCatalog;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentRepository;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentReturnUrlValidator;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializePaymentCommand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializeResult;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentInitializationFailedException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class PaymentServiceTest {

    private static final UUID BUSINESS_PARTNER_ID = UUID.fromString("40f377d5-cd11-458a-944b-0c4f73f2ddaa");

    private PaymentRepository paymentRepository;
    private PaymentReturnUrlValidator paymentReturnUrlValidator;
    private FeaturesProperties featuresProperties;
    private SaferpayClient saferpayClient;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentReturnUrlValidator = mock(PaymentReturnUrlValidator.class);
        featuresProperties = mock(FeaturesProperties.class);
        var businessPartnerService = mock(BusinessPartnerService.class);
        saferpayClient = mock(SaferpayClient.class);
        var paymentProperties = new PaymentProperties(
            List.of("https://portal.example.local"),
            new PaymentProperties.SaferpayProperties(
                "terminal-1",
                "customer-1",
                URI.create("https://test.saferpay.com/api"),
                null
            ),
            List.of(new PaymentProperties.Product("BASE_REGISTRATION", "swiyu Base Registry", 15_000))
        );
        var transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        paymentService = new PaymentService(
            paymentRepository,
            paymentReturnUrlValidator,
            new PaymentProductCatalog(paymentProperties),
            featuresProperties,
            businessPartnerService,
            saferpayClient,
            transactionTemplate
        );

        when(featuresProperties.getEidartfe1297Payment()).thenReturn(true);
        when(paymentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void initiatePayment_featureDisabled_throwsAndNeverCallsSaferpay() {
        when(featuresProperties.getEidartfe1297Payment()).thenReturn(false);

        var request = request();

        assertThatThrownBy(() -> paymentService.initiatePayment(request))
            .isInstanceOf(PaymentInitializationFailedException.class)
            .hasFieldOrPropertyWithValue("errorCode", BusinessExceptionErrorCode.PAYMENT_DISABLED);

        verifyNoInteractions(saferpayClient, paymentRepository);
    }

    @Test
    void initiatePayment_returnUrlNotAllowed_throwsAndNeverCallsSaferpay() {
        doThrow(
            new PaymentInitializationFailedException(
                BusinessExceptionErrorCode.PAYMENT_RETURN_URL_NOT_ALLOWED,
                "not allowed"
            )
        )
            .when(paymentReturnUrlValidator)
            .validate(any());

        var request = request();

        assertThatThrownBy(() -> paymentService.initiatePayment(request))
            .isInstanceOf(PaymentInitializationFailedException.class)
            .hasFieldOrPropertyWithValue("errorCode", BusinessExceptionErrorCode.PAYMENT_RETURN_URL_NOT_ALLOWED);

        verifyNoInteractions(saferpayClient, paymentRepository);
    }

    @Test
    void initiatePayment_happyPath_createsPendingPaymentThenCallsSaferpayThenPersistsResult() {
        var saferpayResult = new SaferpayInitializeResult(
            "token-123",
            "https://saferpay.example/redirect/abc",
            Instant.now().plusSeconds(600)
        );
        when(saferpayClient.initialize(any())).thenReturn(saferpayResult);
        when(paymentRepository.findById(any())).thenAnswer(invocation -> Optional.of(pendingPayment("P-AAAAAAAA")));

        var response = paymentService.initiatePayment(request());

        assertThat(response.businessPartnerId()).isEqualTo(BUSINESS_PARTNER_ID);
        assertThat(response.redirectUrl()).hasToString(saferpayResult.redirectUrl());

        var inOrder = inOrder(paymentRepository, saferpayClient);
        inOrder.verify(paymentRepository).saveAndFlush(any());
        inOrder.verify(saferpayClient).initialize(any());
        inOrder.verify(paymentRepository).findById(any());
    }

    @Test
    void initiatePayment_generatesReferenceIdMatchingExpectedPattern() {
        when(saferpayClient.initialize(any())).thenReturn(
            new SaferpayInitializeResult("token", "https://saferpay.example/redirect", Instant.now())
        );
        when(paymentRepository.findById(any())).thenAnswer(invocation -> Optional.of(pendingPayment("P-BBBBBBBB")));

        paymentService.initiatePayment(request());

        var paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getReferenceId()).matches("^P-[0-9A-F]{8}$");
    }

    @ParameterizedTest
    @CsvSource(
        {
            "https://portal.example.local/payment, https://portal.example.local/payment/%s",
            "https://portal.example.local/payment/, https://portal.example.local/payment/%s",
            "https://portal.example.local/payment?lang=de, https://portal.example.local/payment/%s?lang=de",
        }
    )
    void initiatePayment_appendsPaymentIdAsPathSegmentToReturnUrl(String returnUrl, String expectedPattern)
        throws MalformedURLException {
        when(saferpayClient.initialize(any())).thenReturn(
            new SaferpayInitializeResult("token", "https://saferpay.example/redirect", Instant.now())
        );
        var savedPaymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.saveAndFlush(savedPaymentCaptor.capture())).thenAnswer(invocation ->
            invocation.getArgument(0)
        );
        when(paymentRepository.findById(any())).thenAnswer(invocation -> Optional.of(savedPaymentCaptor.getValue()));

        paymentService.initiatePayment(
            new PaymentInitializationRequestDto(
                BUSINESS_PARTNER_ID,
                ProductTypeDto.BASE_REGISTRATION,
                URI.create(returnUrl).toURL()
            )
        );

        var savedPayment = savedPaymentCaptor.getValue();
        var expectedReturnUrl = expectedPattern.formatted(savedPayment.getId());
        assertThat(savedPayment.getReturnUrl()).isEqualTo(expectedReturnUrl);
        var commandCaptor = ArgumentCaptor.forClass(SaferpayInitializePaymentCommand.class);
        verify(saferpayClient).initialize(commandCaptor.capture());
        assertThat(commandCaptor.getValue().returnUrl()).isEqualTo(expectedReturnUrl);
    }

    @Test
    void initiatePayment_pendingPaymentAlreadyExists_throwsAndNeverCallsSaferpay() {
        when(
            paymentRepository.existsByBusinessPartnerIdAndProductAndStatus(
                BUSINESS_PARTNER_ID,
                ProductType.BASE_REGISTRATION,
                PaymentStatus.PENDING
            )
        ).thenReturn(true);

        var request = request();

        assertThatThrownBy(() -> paymentService.initiatePayment(request))
            .isInstanceOf(PaymentInitializationFailedException.class)
            .hasFieldOrPropertyWithValue("errorCode", BusinessExceptionErrorCode.PAYMENT_ALREADY_PENDING);

        verify(paymentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(saferpayClient);
    }

    @Test
    void initiatePayment_saferpayInitializeFails_marksPaymentFailedAndRethrows() {
        var failure = new IllegalStateException("saferpay down");
        when(saferpayClient.initialize(any())).thenThrow(failure);
        var payment = pendingPayment("P-DDDDDDDD");
        when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));

        var request = request();

        assertThatThrownBy(() -> paymentService.initiatePayment(request)).isSameAs(failure);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void initiatePayment_concurrentPendingPayment_throwsAlreadyPending() {
        // the concurrent request commits between our existence check and our insert: the insert then
        // violates the partial unique index
        when(paymentRepository.saveAndFlush(any())).thenThrow(
            new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"payment_business_partner_id_product_pending_key\""
            )
        );

        var request = request();

        assertThatThrownBy(() -> paymentService.initiatePayment(request))
            .isInstanceOf(PaymentInitializationFailedException.class)
            .hasFieldOrPropertyWithValue("errorCode", BusinessExceptionErrorCode.PAYMENT_ALREADY_PENDING);

        verifyNoInteractions(saferpayClient);
    }

    @Test
    void initiatePayment_otherConstraintViolation_isNotTranslated() {
        var violation = new DataIntegrityViolationException(
            "duplicate key value violates unique constraint \"payment_reference_id_uq\""
        );
        when(paymentRepository.saveAndFlush(any())).thenThrow(violation);

        var request = request();

        assertThatThrownBy(() -> paymentService.initiatePayment(request)).isSameAs(violation);

        verify(paymentRepository, times(1)).saveAndFlush(any());
        verifyNoInteractions(saferpayClient);
    }

    private Payment pendingPayment(String referenceId) {
        return new Payment(
            UUID.randomUUID(),
            BUSINESS_PARTNER_ID,
            referenceId,
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
    }

    private PaymentInitializationRequestDto request() {
        try {
            return new PaymentInitializationRequestDto(
                BUSINESS_PARTNER_ID,
                ProductTypeDto.BASE_REGISTRATION,
                URI.create("https://portal.example.local/callback").toURL()
            );
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
