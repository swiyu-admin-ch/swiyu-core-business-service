package ch.admin.bj.swiyu.core.business.modules.payment.domain.receipt;

import java.io.OutputStream;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Generates a PDF payment receipt from an HTML template using Thymeleaf and Flying-Saucer (OpenPDF).
 * <p>
 * This component is stateless and template-only, making it easily unit-testable without Spring context.
 * Translations are resolved in Java code and passed as context variables to avoid requiring
 * SpringTemplateEngine.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReceiptPdfGenerator {

    private final TemplateEngine receiptTemplateEngine;
    private final MessageSource receiptMessageSource;

    /**
     * Generates a PDF receipt from the given data and writes it to the supplied stream.
     *
     * @param data         the receipt data
     * @param locale       the locale for formatting (already set on data, but passed explicitly for template
     *                     resolution)
     * @param outputStream the stream to write the PDF to; it is not closed by this method
     */
    public void generatePdf(PaymentReceiptData data, Locale locale, OutputStream outputStream) {
        log.debug("Generating PDF receipt for payment {}", data.referenceId());

        // Create Thymeleaf context with all template variables
        Context context = new Context(locale);
        context.setVariable("receipt", data);
        context.setVariable("locale", locale);

        // Add translated messages as context variables
        addTranslatedMessages(context, data, locale);

        // Render HTML from template
        String html;
        try {
            html = receiptTemplateEngine.process("payment-receipt", context);
        } catch (Exception e) {
            log.error("Failed to render receipt template", e);
            throw new PaymentReceiptPdfGenerationException("Failed to render receipt template", e);
        }

        // Convert HTML to PDF using Flying-Saucer
        renderHtmlToPdf(html, outputStream);
    }

    /**
     * Resolves the receipt messages with the receipt MessageSource and exposes them as template variables.
     * The receipt engine is a plain Thymeleaf {@link TemplateEngine} without Spring's MessageSource integration,
     * so the template cannot use the {@code #{...}} message syntax.
     */
    private void addTranslatedMessages(Context context, PaymentReceiptData data, Locale locale) {
        // Letterhead
        context.setVariable(
            "msg_receipt_letterhead_overline",
            getMessage("eportal_payment.receipt.letterhead.overline", locale)
        );
        context.setVariable(
            "msg_receipt_letterhead_headline",
            getMessage("eportal_payment.receipt.letterhead.headline", locale)
        );

        // Recipient / Payee
        context.setVariable(
            "msg_receipt_recipient_headline",
            getMessage("eportal_payment.receipt.recipient.headline", locale)
        );
        context.setVariable(
            "msg_receipt_recipient_department_value",
            getMessage("eportal_payment.receipt.recipient.department", locale)
        );
        context.setVariable(
            "msg_receipt_recipient_division_value",
            getMessage("eportal_payment.receipt.recipient.division", locale)
        );
        context.setVariable(
            "msg_receipt_recipient_street_value",
            getMessage("eportal_payment.receipt.recipient.street", locale)
        );
        context.setVariable(
            "msg_receipt_recipient_city_value",
            getMessage("eportal_payment.receipt.recipient.city", locale)
        );

        // Payment Details
        context.setVariable(
            "msg_receipt_details_headline",
            getMessage("eportal_payment.receipt.details.headline", locale)
        );
        context.setVariable(
            "msg_receipt_details_date_label",
            getMessage("eportal_payment.receipt.details.date", locale)
        );
        context.setVariable(
            "msg_receipt_details_method_label",
            getMessage("eportal_payment.receipt.details.method", locale)
        );
        context.setVariable(
            "msg_receipt_details_provider_label",
            getMessage("eportal_payment.receipt.details.provider", locale)
        );
        context.setVariable(
            "msg_receipt_details_holder_label",
            getMessage("eportal_payment.receipt.details.holder", locale)
        );
        context.setVariable(
            "msg_receipt_details_id_provider_label",
            getMessage("eportal_payment.receipt.details.transaction-id", locale)
        );
        context.setVariable(
            "msg_receipt_details_id_internal_label",
            getMessage("eportal_payment.receipt.details.order-id", locale)
        );

        // Partner ID
        context.setVariable(
            "msg_receipt_partner_id_partner_label",
            getMessage("eportal_payment.receipt.partner.id", locale)
        );

        // Items Overview
        context.setVariable(
            "msg_receipt_overview_headline",
            getMessage("eportal_payment.receipt.overview.headline", locale)
        );
        context.setVariable(
            "msg_receipt_overview_item_label",
            getMessage("eportal_payment.receipt.overview.item", locale)
        );
        context.setVariable(
            "msg_receipt_overview_price_label",
            getMessage("eportal_payment.receipt.overview.price", locale)
        );
        context.setVariable(
            "msg_receipt_overview_sum_label",
            getMessage("eportal_payment.receipt.overview.sum", locale)
        );

        // Per-item description translations (msg_itemDesc_0, msg_itemDesc_1, ...)
        if (data.items() != null) {
            for (var i = 0; i < data.items().size(); i++) {
                var item = data.items().get(i);
                if (item.descriptionKey() != null) {
                    context.setVariable("msg_itemDesc_" + i, getMessage(item.descriptionKey(), locale));
                }
            }
        }
    }

    private String getMessage(String key, Locale locale) {
        try {
            return receiptMessageSource.getMessage(key, null, locale);
        } catch (Exception e) {
            log.warn("Translation missing for key '{}' in locale '{}', using key as fallback", key, locale);
            return key;
        }
    }

    /**
     * Renders HTML string to PDF using Flying-Saucer (OpenPDF).
     *
     * @param html         the rendered HTML
     * @param outputStream the stream to write the PDF to
     */
    private void renderHtmlToPdf(String html, OutputStream outputStream) {
        try {
            ITextRenderer renderer = new ITextRenderer();
            // Provide base URL so relative resources (like the logo) can be resolved from classpath
            var baseUrl = getClass().getResource("/templates/pdf/");
            if (baseUrl != null) {
                renderer.setDocumentFromString(html, baseUrl.toString());
            } else {
                log.warn("Could not find templates/pdf resource directory, rendering without base URL");
                renderer.setDocumentFromString(html);
            }
            renderer.layout();
            renderer.createPDF(outputStream);
        } catch (Exception e) {
            log.error("Failed to generate PDF receipt", e);
            throw new PaymentReceiptPdfGenerationException("Failed to generate PDF receipt", e);
        }
    }

    /**
     * Exception thrown when PDF generation fails.
     */
    public static class PaymentReceiptPdfGenerationException extends RuntimeException {

        public PaymentReceiptPdfGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
