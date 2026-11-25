package ch.admin.bj.swiyu.core.business.modules.payment.domain.receipt;

import static ch.admin.bj.swiyu.core.business.test.PdfTestUtil.extractText;
import static ch.admin.bj.swiyu.core.business.test.PdfTestUtil.writeAndOpenPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentConfig;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class PaymentReceiptPdfGeneratorTest {

    private final PaymentReceiptPdfGenerator generator = new PaymentReceiptPdfGenerator(
        PaymentConfig.createReceiptTemplateEngine(),
        PaymentConfig.createReceiptMessageSource()
    );

    @Test
    void generatePdf_german_containsTranslatedReceiptText() throws Exception {
        // Given
        var data = createSampleData(Locale.GERMAN);

        // When
        var output = new ByteArrayOutputStream();
        generator.generatePdf(data, Locale.GERMAN, output);

        // Then
        var text = extractText(output.toByteArray());
        assertThat(text).contains(
            "Zahlungsbeleg",
            "swiyu Basisregister",
            "Acme GmbH",
            "Musterstrasse 12",
            "150.00",
            "CHF",
            "VISA",
            "J. Doe",
            "P-A3F8B2C1"
        );
    }

    @Test
    void generatePdf_english_containsTranslatedReceiptText() throws Exception {
        // Given
        var data = createSampleData(Locale.ENGLISH);

        // When
        var output = new ByteArrayOutputStream();
        generator.generatePdf(data, Locale.ENGLISH, output);

        // Then
        var text = extractText(output.toByteArray());
        assertThat(text).contains("Payment Receipt", "swiyu Base Registry", "Payment Details", "Acme GmbH", "150.00");
    }

    @Test
    void generatePdf_optionalFieldsNull_stillGeneratesPdf() throws Exception {
        // Given
        var data = createSampleData(Locale.ENGLISH, null, null);

        // When
        var output = new ByteArrayOutputStream();
        generator.generatePdf(data, Locale.ENGLISH, output);

        // Then
        var pdfBytes = output.toByteArray();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        assertThat(extractText(pdfBytes)).contains("Acme GmbH", "P-A3F8B2C1");
    }

    @Test
    void generatePdf_whenTemplateEngineFails_wrapsInCustomException() {
        // Given: a template engine whose resolver cannot find the receipt template
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("does-not-exist/");
        resolver.setSuffix(".html");
        var failingEngine = new TemplateEngine();
        failingEngine.setTemplateResolver(resolver);
        var failingGenerator = new PaymentReceiptPdfGenerator(
            failingEngine,
            PaymentConfig.createReceiptMessageSource()
        );
        var data = createSampleData(Locale.ENGLISH);

        // When / Then
        assertThatThrownBy(() -> failingGenerator.generatePdf(data, Locale.ENGLISH, new ByteArrayOutputStream()))
            .isInstanceOf(PaymentReceiptPdfGenerator.PaymentReceiptPdfGenerationException.class)
            .hasMessageContaining("Failed to render receipt template");
    }

    @Disabled("Local development helper: renders the receipt PDF and opens it in the desktop viewer")
    @Test
    void generatePdf_localDevelopment_opensPdf() throws Exception {
        var output = new ByteArrayOutputStream();
        generator.generatePdf(createSampleData(Locale.GERMAN), Locale.GERMAN, output);
        writeAndOpenPdf(output.toByteArray());
    }

    private PaymentReceiptData createSampleData(Locale locale) {
        return createSampleData(locale, "J. Doe", "TXN-12345");
    }

    private PaymentReceiptData createSampleData(Locale locale, String holdername, String transactionId) {
        var item = new PaymentReceiptData.Item(
            "swiyu Base Registration",
            "eportal_global_product.BR_name.localized",
            1,
            new BigDecimal("150.00"),
            new BigDecimal("150.00")
        );

        return new PaymentReceiptData(
            "P-A3F8B2C1",
            Instant.now(),
            "Acme GmbH",
            "CHE-123.456.789",
            "a3f31a611c6f",
            "Musterstrasse 12, 3000 Bern, Switzerland",
            List.of(item),
            "VISA",
            "Saferpay",
            holdername,
            transactionId,
            locale,
            new BigDecimal("150.00"),
            "CHF"
        );
    }
}
