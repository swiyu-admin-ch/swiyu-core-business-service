package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.exceptions.PaymentReceiptNotAvailableException;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.payment.api.*;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentDisabledException;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentReceiptPdfService;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentService;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;

class PaymentInternalControllerTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentReceiptPdfService paymentReceiptPdfService = mock(PaymentReceiptPdfService.class);
    private final PaymentInternalController controller;

    PaymentInternalControllerTest() {
        var functionality = new FunctionalityProperties(true, false);
        controller = new PaymentInternalController(
            paymentService,
            paymentReceiptPdfService,
            mock(AuthSupport.class),
            functionality
        );
    }

    @Test
    void featureDisabled_allEndpointsRefuseAndNeverCallService() throws MalformedURLException {
        var filter = new PaymentFilterDto(null, null, null);
        var pageable = Pageable.unpaged();
        var paymentId = UUID.randomUUID();
        var request = new PaymentInitializationRequestDto(
            UUID.randomUUID(),
            ProductTypeDto.BASE_REGISTRATION,
            URI.create("https://portal.example.local/callback").toURL()
        );

        assertThatThrownBy(() -> controller.getPayments(filter, pageable)).isInstanceOf(PaymentDisabledException.class);
        assertThatThrownBy(() -> controller.getPayment(paymentId)).isInstanceOf(PaymentDisabledException.class);
        var retryRequest = new PaymentRetryRequestDto(paymentId, UUID.randomUUID());

        assertThatThrownBy(() -> controller.initiatePayment(request)).isInstanceOf(PaymentDisabledException.class);
        assertThatThrownBy(() -> controller.retryPayment(retryRequest)).isInstanceOf(PaymentDisabledException.class);
        var randomUUID = UUID.randomUUID();
        var mockHttpServletResponse = new MockHttpServletResponse();
        assertThatThrownBy(() ->
            controller.getPaymentReceipt(randomUUID, paymentId, null, mockHttpServletResponse)
        ).isInstanceOf(PaymentDisabledException.class);
        verifyNoInteractions(paymentService);
        verifyNoInteractions(paymentReceiptPdfService);
    }

    @Test
    void getPaymentReceipt_featureEnabled_paymentNotFound_throwsResourceNotFound() {
        var functionality = new FunctionalityProperties(true, true);
        var enabledController = new PaymentInternalController(
            paymentService,
            paymentReceiptPdfService,
            mock(AuthSupport.class),
            functionality
        );

        var businessPartnerId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();

        when(paymentService.getPayment(paymentId)).thenThrow(
            new ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException("Not found")
        );

        assertThatThrownBy(() ->
            enabledController.getPaymentReceipt(businessPartnerId, paymentId, "de", new MockHttpServletResponse())
        ).isInstanceOf(ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException.class);
    }

    @Test
    void getPaymentReceipt_featureEnabled_paymentNotSucceeded_throwsPaymentReceiptNotAvailable() {
        var functionality = new FunctionalityProperties(true, true);
        var enabledController = new PaymentInternalController(
            paymentService,
            paymentReceiptPdfService,
            mock(AuthSupport.class),
            functionality
        );

        var businessPartnerId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();

        PaymentDto paymentDto = new PaymentDto(
            paymentId,
            businessPartnerId,
            "P-A3F8B2C1",
            PaymentStatusDto.PENDING,
            ProductTypeDto.BASE_REGISTRATION,
            "https://portal.example.com/payment",
            null,
            new AmountDto(15000L, "CHF"),
            Instant.now(),
            Instant.now()
        );

        when(paymentService.getPayment(paymentId)).thenReturn(paymentDto);

        var mockHttpServletResponse = new MockHttpServletResponse();
        assertThatThrownBy(() ->
            enabledController.getPaymentReceipt(businessPartnerId, paymentId, "de", mockHttpServletResponse)
        ).isInstanceOf(PaymentReceiptNotAvailableException.class);
    }

    @Test
    void getPaymentReceipt_featureEnabled_succeededPayment_returnsPdf() throws Exception {
        var functionality = new FunctionalityProperties(true, true);
        var enabledController = new PaymentInternalController(
            paymentService,
            paymentReceiptPdfService,
            mock(AuthSupport.class),
            functionality
        );

        var businessPartnerId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();

        PaymentDto paymentDto = new PaymentDto(
            paymentId,
            businessPartnerId,
            "P-A3F8B2C1",
            PaymentStatusDto.SUCCEEDED,
            ProductTypeDto.BASE_REGISTRATION,
            "https://portal.example.com/payment",
            null,
            new AmountDto(15000L, "CHF"),
            Instant.now(),
            Instant.now()
        );

        when(paymentService.getPayment(paymentId)).thenReturn(paymentDto);
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(3);
            outputStream.write("PDF_CONTENT".getBytes());
            return null;
        })
            .when(paymentReceiptPdfService)
            .writeReceipt(eq(paymentId), eq(businessPartnerId), eq(Language.DE), any(OutputStream.class));

        var response = new MockHttpServletResponse();
        enabledController.getPaymentReceipt(businessPartnerId, paymentId, "de", response);

        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getHeader("Content-Disposition")).isEqualTo(
            "attachment; filename=\"receipt-P-A3F8B2C1.pdf\""
        );
        assertThat(response.getContentAsByteArray()).isEqualTo("PDF_CONTENT".getBytes());
    }

    @Test
    void getPaymentReceipt_localeDeCh_usesGerman() throws Exception {
        assertReceiptLocale("de-CH", Language.DE);
    }

    @Test
    void getPaymentReceipt_localeNull_usesEnglish() throws Exception {
        assertReceiptLocale(null, Language.EN);
    }

    @Test
    void getPaymentReceipt_localeSpanish_usesEnglish() throws Exception {
        assertReceiptLocale("es", Language.EN);
    }

    private void assertReceiptLocale(String requestedLocale, Language expectedLanguage) throws Exception {
        var businessPartnerId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        when(paymentService.getPayment(paymentId)).thenReturn(succeededPayment(paymentId, businessPartnerId));

        enabledController().getPaymentReceipt(
            businessPartnerId,
            paymentId,
            requestedLocale,
            new MockHttpServletResponse()
        );

        verify(paymentReceiptPdfService).writeReceipt(
            eq(paymentId),
            eq(businessPartnerId),
            eq(expectedLanguage),
            any(OutputStream.class)
        );
    }

    private PaymentInternalController enabledController() {
        var functionality = new FunctionalityProperties(true, true);
        return new PaymentInternalController(
            paymentService,
            paymentReceiptPdfService,
            mock(AuthSupport.class),
            functionality
        );
    }

    private PaymentDto succeededPayment(UUID paymentId, UUID businessPartnerId) {
        return new PaymentDto(
            paymentId,
            businessPartnerId,
            "P-A3F8B2C1",
            PaymentStatusDto.SUCCEEDED,
            ProductTypeDto.BASE_REGISTRATION,
            "https://portal.example.com/payment",
            null,
            new AmountDto(15000L, "CHF"),
            Instant.now(),
            Instant.now()
        );
    }
}
