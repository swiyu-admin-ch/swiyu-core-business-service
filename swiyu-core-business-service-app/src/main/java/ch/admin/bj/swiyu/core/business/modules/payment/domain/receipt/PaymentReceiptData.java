package ch.admin.bj.swiyu.core.business.modules.payment.domain.receipt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * All information needed to render a payment receipt PDF.
 * <p>
 * This is an internal template model, not an API DTO: it is only passed from the receipt service to the
 * PDF generator.
 */
public record PaymentReceiptData(
    String referenceId,
    Instant payedAt,
    String businessPartnerName,
    String businessPartnerUid,
    String businessPartnerId,
    String businessPartnerAddress,
    List<Item> items,
    String paymentMethod,
    String paymentProvider,
    String holdername,
    String transactionId,
    Locale locale,
    BigDecimal totalAmount,
    String currency
) {
    /**
     * Individual line item on the receipt.
     */
    public record Item(
        String description,
        String descriptionKey,
        int quantity,
        BigDecimal unitAmount,
        BigDecimal totalAmount
    ) {}
}
