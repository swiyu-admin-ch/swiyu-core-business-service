package ch.admin.bj.swiyu.core.business.modules.payment.service;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerDto;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentRepository;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.receipt.PaymentReceiptData;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.receipt.PaymentReceiptPdfGenerator;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating payment receipt PDFs.
 * Loads payment and business partner data, builds the receipt DTO, and delegates PDF generation.
 * <p>
 * All products are VAT-free, so no VAT calculation is needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReceiptPdfService {

    private static final Set<Language> SUPPORTED_RECEIPT_LANGUAGES = EnumSet.of(
        Language.DE,
        Language.FR,
        Language.IT,
        Language.EN
    );

    private final PaymentRepository paymentRepository;
    private final BusinessPartnerService businessPartnerService;
    private final PaymentReceiptPdfGenerator pdfGenerator;
    private final MessageSource receiptMessageSource;

    /**
     * Writes a payment receipt PDF for a successfully paid payment to the supplied stream.
     *
     * @param paymentId         the payment ID
     * @param businessPartnerId the business partner ID (for security - ensures user can only access their own
     *                          receipts)
     * @param requestedLanguage the requested receipt language; unsupported languages fall back to English
     * @param outputStream      the stream to write the PDF to
     * @throws ResourceNotFoundException if payment not found
     * @throws IllegalStateException     if payment status is not SUCCEEDED
     */
    @Transactional(readOnly = true)
    public void writeReceipt(
        UUID paymentId,
        UUID businessPartnerId,
        Language requestedLanguage,
        OutputStream outputStream
    ) {
        log.debug("Generating receipt for payment {} for business partner {}", paymentId, businessPartnerId);

        Language language = SUPPORTED_RECEIPT_LANGUAGES.contains(requestedLanguage) ? requestedLanguage : Language.EN;
        Locale locale = language.getSwissLocale();

        // Load payment by BOTH paymentId AND businessPartnerId for security
        Payment payment = paymentRepository
            .findByIdAndBusinessPartnerId(paymentId, businessPartnerId)
            .orElseThrow(() -> paymentNotFound(paymentId));

        // Only allow receipt generation for SUCCEEDED payments
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException(
                "Receipt can only be generated for SUCCEEDED payments. Current status: " + payment.getStatus()
            );
        }

        // Load business partner for address and name
        var businessPartner = businessPartnerService.getBusinessPartner(businessPartnerId);

        // Build receipt data
        PaymentReceiptData data = buildReceiptData(payment, businessPartner, locale, language);

        // Generate PDF
        pdfGenerator.generatePdf(data, locale, outputStream);
    }

    /**
     * Builds the PaymentReceiptData from payment and business partner data.
     */
    private PaymentReceiptData buildReceiptData(
        Payment payment,
        BusinessPartnerDto businessPartner,
        Locale locale,
        Language language
    ) {
        // Build items list
        List<PaymentReceiptData.Item> items = buildItems(payment, locale);

        // Calculate totals - Amount stores value in minor units (rappen), convert to BigDecimal
        BigDecimal totalAmount = BigDecimal.valueOf(payment.getAmount().getValue()).scaleByPowerOfTen(-2);
        String currency = payment.getAmount().getCurrency();

        // Get business partner name in the requested language
        String businessPartnerName = LocalizedMapUtil.getByLanguageOrDefault(businessPartner.entityName(), language);

        // Payment provider is Saferpay (the only supported provider currently)
        var paymentProvider = "Saferpay";
        // Saferpay transaction ID for the receipt
        String transactionId = payment.getSaferpayTransactionId();

        return new PaymentReceiptData(
            payment.getReferenceId(),
            payment.getPayedAt(),
            businessPartnerName,
            businessPartner.uid(),
            payment.getBusinessPartnerId().toString(),
            formatAddress(businessPartner.address()),
            items,
            payment.getPaymentMethod(),
            paymentProvider,
            payment.getHoldername(),
            transactionId,
            locale,
            totalAmount,
            currency
        );
    }

    /**
     * Builds the receipt items list based on the payment product.
     * For now, there's a single item per payment (the product purchased).
     * All products are VAT-free.
     */
    private List<PaymentReceiptData.Item> buildItems(Payment payment, Locale locale) {
        List<PaymentReceiptData.Item> items = new ArrayList<>();

        ProductType product = payment.getProduct();
        String descriptionKey = getProductDescriptionKey(product);
        String description = receiptMessageSource.getMessage(descriptionKey, null, descriptionKey, locale);
        // Amount stores value in minor units (rappen), convert to BigDecimal
        BigDecimal totalAmount = BigDecimal.valueOf(payment.getAmount().getValue()).scaleByPowerOfTen(-2);

        items.add(new PaymentReceiptData.Item(description, descriptionKey, 1, totalAmount, totalAmount));

        return items;
    }

    /**
     * Gets the translation key for the product description.
     */
    private String getProductDescriptionKey(ProductType product) {
        return switch (product) {
            case BASE_REGISTRATION -> "eportal_global_product.BR_name.localized";
            // Future products:
            // case TRUST_REGISTRATION -> "eportal_global_product.TR_name.localized";
            // case DID -> "eportal_global_product.DID_name";
            default -> "eportal_global_product." + product.name() + "_name.localized";
        };
    }

    /**
     * Formats the business partner address as a single line.
     */
    private String formatAddress(AddressDto addr) {
        if (addr == null) {
            return "";
        }
        var sb = new StringBuilder();
        if (addr.street() != null) {
            sb.append(addr.street());
        }
        if (sb.length() > 0) sb.append(", ");
        if (addr.postalCode() != null) {
            sb.append(addr.postalCode()).append(" ");
        }
        if (addr.city() != null) {
            sb.append(addr.city());
        }
        if (addr.country() != null) {
            sb.append(", ").append(addr.country());
        }
        return sb.toString();
    }

    private static ResourceNotFoundException paymentNotFound(UUID paymentId) {
        return new ResourceNotFoundException("No payment found with id " + paymentId);
    }
}
