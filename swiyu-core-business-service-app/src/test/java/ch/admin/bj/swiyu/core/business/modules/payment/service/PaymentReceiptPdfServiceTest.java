package ch.admin.bj.swiyu.core.business.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerDto;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.*;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.receipt.PaymentReceiptData;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.receipt.PaymentReceiptPdfGenerator;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptPdfServiceTest {

    private static final String PRODUCT_NAME_KEY = "eportal_global_product.BR_name.localized";

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    BusinessPartnerService businessPartnerService;

    @Mock
    PaymentReceiptPdfGenerator pdfGenerator;

    @Mock
    MessageSource receiptMessageSource;

    @InjectMocks
    PaymentReceiptPdfService service;

    @Test
    void generateReceipt_forSucceededPayment_returnsPdf() {
        // Given
        var paymentId = UUID.randomUUID();
        var businessPartnerId = UUID.randomUUID();
        var language = Language.EN;
        var locale = language.getSwissLocale();

        Payment payment = createPayment(paymentId, businessPartnerId, PaymentStatus.SUCCEEDED);
        BusinessPartnerDto businessPartner = createBusinessPartnerDto(businessPartnerId);
        var expectedPdf = "PDF_CONTENT".getBytes();

        when(paymentRepository.findByIdAndBusinessPartnerId(paymentId, businessPartnerId)).thenReturn(
            Optional.of(payment)
        );
        when(businessPartnerService.getBusinessPartner(businessPartnerId)).thenReturn(businessPartner);
        when(receiptMessageSource.getMessage(PRODUCT_NAME_KEY, null, PRODUCT_NAME_KEY, locale)).thenReturn(
            "swiyu Base Registry"
        );
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(2);
            outputStream.write(expectedPdf);
            return null;
        })
            .when(pdfGenerator)
            .generatePdf(any(PaymentReceiptData.class), eq(locale), any(OutputStream.class));

        // When
        var output = new ByteArrayOutputStream();
        service.writeReceipt(paymentId, businessPartnerId, language, output);

        // Then
        assertThat(output.toByteArray()).isEqualTo(expectedPdf);
    }

    @Test
    void generateReceipt_forFailedPayment_throwsIllegalStateException() {
        // Given
        var paymentId = UUID.randomUUID();
        var businessPartnerId = UUID.randomUUID();

        Payment payment = createPayment(paymentId, businessPartnerId, PaymentStatus.FAILED);

        when(paymentRepository.findByIdAndBusinessPartnerId(paymentId, businessPartnerId)).thenReturn(
            Optional.of(payment)
        );

        // When / Then
        var byteArrayOutputStream = new ByteArrayOutputStream();
        assertThatThrownBy(() -> service.writeReceipt(paymentId, businessPartnerId, Language.EN, byteArrayOutputStream))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SUCCEEDED");
    }

    @Test
    void generateReceipt_forPendingPayment_throwsIllegalStateException() {
        // Given
        var paymentId = UUID.randomUUID();
        var businessPartnerId = UUID.randomUUID();

        Payment payment = createPayment(paymentId, businessPartnerId, PaymentStatus.PENDING);

        when(paymentRepository.findByIdAndBusinessPartnerId(paymentId, businessPartnerId)).thenReturn(
            Optional.of(payment)
        );

        // When / Then
        var byteArrayOutputStream = new ByteArrayOutputStream();
        assertThatThrownBy(() -> service.writeReceipt(paymentId, businessPartnerId, Language.EN, byteArrayOutputStream))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SUCCEEDED");
    }

    @Test
    void generateReceipt_forCancelledPayment_throwsIllegalStateException() {
        // Given
        var paymentId = UUID.randomUUID();
        var businessPartnerId = UUID.randomUUID();

        Payment payment = createPayment(paymentId, businessPartnerId, PaymentStatus.CANCELLED);

        when(paymentRepository.findByIdAndBusinessPartnerId(paymentId, businessPartnerId)).thenReturn(
            Optional.of(payment)
        );

        // When / Then
        var byteArrayOutputStream = new ByteArrayOutputStream();
        assertThatThrownBy(() -> service.writeReceipt(paymentId, businessPartnerId, Language.EN, byteArrayOutputStream))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SUCCEEDED");
    }

    @Test
    void generateReceipt_paymentNotFound_throwsResourceNotFoundException() {
        // Given
        var paymentId = UUID.randomUUID();
        var businessPartnerId = UUID.randomUUID();

        when(paymentRepository.findByIdAndBusinessPartnerId(paymentId, businessPartnerId)).thenReturn(Optional.empty());

        // When / Then
        var byteArrayOutputStream = new ByteArrayOutputStream();
        assertThatThrownBy(() ->
            service.writeReceipt(paymentId, businessPartnerId, Language.EN, byteArrayOutputStream)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateReceipt_wrongBusinessPartner_throwsResourceNotFoundException() {
        // Given
        var paymentId = UUID.randomUUID();
        var businessPartnerId = UUID.randomUUID();
        var otherBusinessPartnerId = UUID.randomUUID();

        createPayment(paymentId, businessPartnerId, PaymentStatus.SUCCEEDED);

        when(paymentRepository.findByIdAndBusinessPartnerId(paymentId, otherBusinessPartnerId)).thenReturn(
            Optional.empty()
        );

        // When / Then
        var byteArrayOutputStream = new ByteArrayOutputStream();
        assertThatThrownBy(() ->
            service.writeReceipt(paymentId, otherBusinessPartnerId, Language.EN, byteArrayOutputStream)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateReceipt_withGermanLocale_returnsPdf() {
        // Given
        var paymentId = UUID.randomUUID();
        var businessPartnerId = UUID.randomUUID();
        var language = Language.DE;
        var locale = language.getSwissLocale();

        Payment payment = createPayment(
            paymentId,
            businessPartnerId,
            PaymentStatus.SUCCEEDED,
            ProductType.BASE_REGISTRATION
        );
        BusinessPartnerDto businessPartner = createBusinessPartnerDto(businessPartnerId);
        var expectedPdf = "PDF_CONTENT".getBytes();

        when(paymentRepository.findByIdAndBusinessPartnerId(paymentId, businessPartnerId)).thenReturn(
            Optional.of(payment)
        );
        when(businessPartnerService.getBusinessPartner(businessPartnerId)).thenReturn(businessPartner);
        when(receiptMessageSource.getMessage(PRODUCT_NAME_KEY, null, PRODUCT_NAME_KEY, locale)).thenReturn(
            "swiyu Basisregister"
        );
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(2);
            outputStream.write(expectedPdf);
            return null;
        })
            .when(pdfGenerator)
            .generatePdf(any(PaymentReceiptData.class), eq(locale), any(OutputStream.class));

        // When
        var output = new ByteArrayOutputStream();
        service.writeReceipt(paymentId, businessPartnerId, language, output);

        // Then
        assertThat(output.toByteArray()).isEqualTo(expectedPdf);
    }

    @Test
    void writeReceipt_unsupportedLanguage_fallsBackToEnglish() {
        // Given
        var paymentId = UUID.randomUUID();
        var businessPartnerId = UUID.randomUUID();
        var englishLocale = Language.EN.getSwissLocale();

        Payment payment = createPayment(paymentId, businessPartnerId, PaymentStatus.SUCCEEDED);
        BusinessPartnerDto businessPartner = createBusinessPartnerDto(
            businessPartnerId,
            Map.of("default", "Fallback GmbH", "de-CH", "Deutsche GmbH", "en-CH", "English Ltd")
        );

        when(paymentRepository.findByIdAndBusinessPartnerId(paymentId, businessPartnerId)).thenReturn(
            Optional.of(payment)
        );
        when(businessPartnerService.getBusinessPartner(businessPartnerId)).thenReturn(businessPartner);
        when(receiptMessageSource.getMessage(PRODUCT_NAME_KEY, null, PRODUCT_NAME_KEY, englishLocale)).thenReturn(
            "swiyu Base Registry"
        );
        doAnswer(invocation -> null)
            .when(pdfGenerator)
            .generatePdf(any(PaymentReceiptData.class), eq(englishLocale), any(OutputStream.class));

        // When
        service.writeReceipt(paymentId, businessPartnerId, Language.RM, new ByteArrayOutputStream());

        // Then
        var captor = ArgumentCaptor.forClass(PaymentReceiptData.class);
        verify(pdfGenerator).generatePdf(captor.capture(), eq(englishLocale), any(OutputStream.class));
        assertThat(captor.getValue().locale()).isEqualTo(englishLocale);
        assertThat(captor.getValue().businessPartnerName()).isEqualTo("English Ltd");
    }

    private Payment createPayment(UUID paymentId, UUID businessPartnerId, PaymentStatus status) {
        return createPayment(paymentId, businessPartnerId, status, ProductType.BASE_REGISTRATION);
    }

    private Payment createPayment(UUID paymentId, UUID businessPartnerId, PaymentStatus status, ProductType product) {
        Payment payment = new Payment(
            paymentId,
            businessPartnerId,
            "P-A3F8B2C1",
            product,
            Amount.chf(15000L), // 150.00 CHF in minor units (rappen)
            "https://portal.example.com/payment"
        );
        // Use reflection to set status and receipt fields since they're private
        try {
            var statusField = Payment.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(payment, status);

            var payedAtField = Payment.class.getDeclaredField("payedAt");
            payedAtField.setAccessible(true);
            payedAtField.set(payment, Instant.now());

            var paymentMethodField = Payment.class.getDeclaredField("paymentMethod");
            paymentMethodField.setAccessible(true);
            paymentMethodField.set(payment, "VISA");

            var holdernameField = Payment.class.getDeclaredField("holdername");
            holdernameField.setAccessible(true);
            holdernameField.set(payment, "J. Doe");

            var saferpayTransactionIdField = Payment.class.getDeclaredField("saferpayTransactionId");
            saferpayTransactionIdField.setAccessible(true);
            saferpayTransactionIdField.set(payment, "TXN-12345");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set payment fields", e);
        }
        return payment;
    }

    private BusinessPartnerDto createBusinessPartnerDto(UUID id) {
        return createBusinessPartnerDto(id, Map.of("default", "Acme GmbH", "de-CH", "Acme GmbH", "en-CH", "Acme GmbH"));
    }

    private BusinessPartnerDto createBusinessPartnerDto(UUID id, Map<String, String> entityName) {
        return new BusinessPartnerDto(
            id,
            entityName,
            BusinessPartnerTypeDto.BUSINESS,
            false,
            0,
            Instant.now(),
            Instant.now(),
            "CHE-123.456.789",
            new AddressDto("Musterstrasse", "3000", "Bern", "CH"),
            null,
            null,
            null,
            null,
            true
        );
    }
}
