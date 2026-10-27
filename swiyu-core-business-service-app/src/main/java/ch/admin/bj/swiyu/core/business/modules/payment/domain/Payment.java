package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A payment for a product, initiated with Saferpay. Some fields (status transitions to
 * SUCCEEDED/FAILED, saferpayTransactionId, inventoryGrantedAt, payedAt, holdername,
 * paymentMethod) are only populated by later stages of the payment lifecycle
 * (notify/assert/capture) which are not yet implemented.
 */
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    private UUID id;

    @NotNull
    private UUID businessPartnerId;

    /**
     * Human-readable id of the payment, e.g. "P-A3F8B2C1", created by PaymentReferenceIdGenerator.
     * It is sent to Saferpay as orderId, so the payment can be found under this id in the Saferpay
     * Backoffice, and it is the number shown to the user (e.g. on the receipt). The technical key of the
     * payment in our API and database is the id above, not this one.
     */
    @NotNull
    @Column(unique = true)
    private String referenceId;

    @Version
    private Long version;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProductType product;

    /**
     * The page of our frontend (portal) where the user should land after paying on Saferpay. The frontend
     * sends this URL when it starts the payment, e.g. "https://portal.../payment".
     *
     * Stored as received. The URL actually given to Saferpay is this one plus "/{paymentId}".
     *
     * Why we store it: if the user closes the browser on the Saferpay page and comes back later, we still
     * know which page to send them to.
     */
    @NotNull
    private String returnUrl;

    /** Saferpay's handle for the payment page session, needed to ask Saferpay for the outcome of the payment. */
    private String saferpayToken;

    private Instant saferpayExpiration;

    private String saferpayRedirectUrl;

    private String saferpayTransactionId;

    /** Idempotence flag for inventory grants. Unused until the finalize-payment ticket. */
    private Instant inventoryGrantedAt;

    /** Receipt-relevant fields. Unused until the finalize-payment ticket. */
    private Instant payedAt;

    private String holdername;

    private String paymentMethod;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "amount_value"))
    @AttributeOverride(name = "currency", column = @Column(name = "amount_currency"))
    @NotNull
    @Valid
    private Amount amount;

    protected Payment() {
        // JPA
    }

    public Payment(
        UUID paymentId,
        UUID businessPartnerId,
        String referenceId,
        ProductType product,
        Amount amount,
        String returnUrl
    ) {
        this.id = paymentId;
        this.businessPartnerId = businessPartnerId;
        this.referenceId = referenceId;
        this.product = product;
        this.amount = amount;
        this.returnUrl = returnUrl;
        this.status = PaymentStatus.PENDING;
    }

    /** Applied once the Saferpay PaymentPage/Initialize call has returned. */
    public void applySaferpayInitialization(String token, Instant expiration, String redirectUrl) {
        this.saferpayToken = token;
        this.saferpayExpiration = expiration;
        this.saferpayRedirectUrl = redirectUrl;
    }

    /** Applied when the payment could not be completed, e.g. the Saferpay PaymentPage/Initialize call failed. */
    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
    }

    /** Only for demo data until the finalize-payment ticket implements the real transition. */
    @VisibleForTesting
    public void markAsSucceeded(Instant payedAt) {
        this.status = PaymentStatus.SUCCEEDED;
        this.payedAt = payedAt;
    }

    /** Only for demo data until the cancel/abort handling implements the real transition. */
    @VisibleForTesting
    public void markAsCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }
}
