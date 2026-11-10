package ch.admin.bj.swiyu.core.business.modules.payment.service;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.audit.PaymentAuditDto;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentFilterDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationResponseDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentProductCatalog;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentReferenceIdGenerator;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentRepository;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentReturnUrlValidator;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.QPayment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.event.TiPaymentChangeNotificationReceivedEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.event.TiPaymentSucceededEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayAssertCommand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayAssertResult;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayCancelCommand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayCaptureCommand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayCaptureResult;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializePaymentCommand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializeResult;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayTransactionStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayTransaction;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayTransactionReference;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentFinalizationFailedException;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentInitializationFailedException;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentRetryNotAllowedException;
import ch.admin.bj.swiyu.messagetype.ti.TiPaymentChangeNotificationReceivedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiPaymentSucceededEvent;
import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class PaymentService {

    /** Partial unique index of the payment table: at most one PENDING payment per business partner and product. */
    private static final String PENDING_PAYMENT_UNIQUE_INDEX = "payment_business_partner_id_product_pending_key";

    private final PaymentRepository paymentRepository;
    private final PaymentReturnUrlValidator paymentReturnUrlValidator;
    private final PaymentProductCatalog paymentProductCatalog;
    private final BusinessPartnerService businessPartnerService;
    private final SaferpayClient saferpayClient;
    private final TransactionalOutbox transactionalOutbox;
    private final TransactionTemplate transactionTemplate;

    @Qualifier("transactionTemplateRequiresNew")
    private TransactionTemplate transactionTemplateRequiresNew;

    private final EntityManager entityManager;
    private final AuditPublisher auditPublisher;

    @Autowired
    public PaymentService(
        PaymentRepository paymentRepository,
        PaymentReturnUrlValidator paymentReturnUrlValidator,
        PaymentProductCatalog paymentProductCatalog,
        BusinessPartnerService businessPartnerService,
        SaferpayClient saferpayClient,
        TransactionalOutbox transactionalOutbox,
        TransactionTemplate transactionTemplate,
        @Autowired(required = false) @Qualifier(
            "transactionTemplateRequiresNew"
        ) TransactionTemplate transactionTemplateRequiresNew,
        EntityManager entityManager,
        AuditPublisher auditPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentReturnUrlValidator = paymentReturnUrlValidator;
        this.paymentProductCatalog = paymentProductCatalog;
        this.businessPartnerService = businessPartnerService;
        this.saferpayClient = saferpayClient;
        this.transactionalOutbox = transactionalOutbox;
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplateRequiresNew = transactionTemplateRequiresNew;
        this.entityManager = entityManager;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Initiates a payment for the requested product at its current catalog price, see startPayment.
     */
    public PaymentInitializationResponseDto initiatePayment(PaymentInitializationRequestDto request) {
        // the returnUrl is supplied by the caller - only accept it if it points to one of our own frontends
        paymentReturnUrlValidator.validate(request.returnUrl());
        businessPartnerService.validateBusinessPartnerExists(request.businessPartnerId());

        var product = PaymentMapper.toProductType(request.product());
        return startPayment(
            request.businessPartnerId(),
            product,
            Amount.chf(paymentProductCatalog.priceFor(product)),
            request.returnUrl().toString()
        );
    }

    /**
     * Starts a new payment for a payment that failed or was cancelled, so the user can pay again.
     *
     * The previous payment is not changed: a retry always creates a new payment, because every attempt
     * needs its own referenceId (Saferpay orderId) and its own Saferpay token. Product, amount and
     * currency are copied from the previous payment, and the browser is sent back to the same frontend
     * page, now with the id of the new payment as last path segment.
     *
     * A PENDING payment is refused, because paying it is still possible via its own redirectUrl, and a
     * SUCCEEDED payment is refused, because it must not be paid twice (409 Conflict).
     */
    public PaymentInitializationResponseDto retryPayment(UUID previousPaymentId, UUID businessPartnerId) {
        // fetch by both ids, so a caller cannot retry a payment of another business partner
        var previous = paymentRepository
            .findByIdAndBusinessPartnerId(previousPaymentId, businessPartnerId)
            .orElseThrow(() -> paymentNotFound(previousPaymentId));
        ensureRetryable(previous);

        return startPayment(
            previous.getBusinessPartnerId(),
            previous.getProduct(),
            new Amount(previous.getAmount().getValue(), previous.getAmount().getCurrency()),
            returnUrlWithoutPaymentId(previous)
        );
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(UUID paymentId) {
        return PaymentMapper.toPaymentDto(findPayment(paymentId));
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getPayments(PaymentFilterDto filter, Pageable pageable) {
        var payment = QPayment.payment;
        var where = new BooleanBuilder();
        if (filter.businessPartnerIds() != null) {
            where.and(payment.businessPartnerId.in(filter.businessPartnerIds()));
        }
        if (filter.productTypes() != null) {
            where.and(payment.product.in(filter.productTypes().stream().map(PaymentMapper::toProductType).toList()));
        }
        if (filter.statuses() != null) {
            where.and(payment.status.in(filter.statuses().stream().map(PaymentMapper::toPaymentStatus).toList()));
        }
        return paymentRepository
            .findAll(
                where,
                PageableUtils.toDbPageableFromUserPageable(
                    PaymentDto.class,
                    Payment.class,
                    pageable,
                    Map.of("amount", "amount.value")
                )
            )
            .map(PaymentMapper::toPaymentDto);
    }

    /**
     * Without a payment page the PENDING payment can never be paid, and as long as it stays PENDING it
     * blocks every further payment for the same business partner and product. Therefore it is marked
     * FAILED when Saferpay cannot be reached or rejects the request.
     */
    private SaferpayInitializeResult initializeSaferpayPayment(
        UUID paymentId,
        SaferpayInitializePaymentCommand command
    ) {
        try {
            return saferpayClient.initialize(command);
        } catch (RuntimeException e) {
            transactionTemplate.execute(status -> markAsFailed(paymentId));
            throw e;
        }
    }

    /**
     * Saves the PENDING payment in its own transaction.
     *
     * Special case double click: two requests (A and B) arrive at the same time.
     * 1. Both pass the "is there already a pending payment?" check, because neither has saved anything yet.
     * 2. Both try to insert their payment. The database lets A in first and makes B wait, because only
     *    one PENDING payment per business partner and product is allowed (unique index).
     * 3. As soon as A is committed, the database rejects B's insert with an error naming that index.
     * 4. We recognize the index name and answer B with the same error as the check itself (HTTP 400,
     *    payment already pending) instead of a technical error (HTTP 500).
     *
     * So the unique index is the real guarantee; the check in the code only gives the normal case (first
     * payment already saved) a clean error without relying on a database exception.
     */
    private Payment createPendingPaymentInTransaction(
        UUID businessPartnerId,
        ProductType product,
        Amount amount,
        String baseReturnUrl
    ) {
        try {
            return transactionTemplate.execute(status ->
                createPendingPayment(businessPartnerId, product, amount, baseReturnUrl)
            );
        } catch (DataIntegrityViolationException e) {
            if (isPendingPaymentIndexViolation(e)) {
                throw alreadyPending(businessPartnerId, product);
            }
            // Anything else is unexpected, e.g. the (very unlikely) collision of the random referenceId.
            throw e;
        }
    }

    private static boolean isPendingPaymentIndexViolation(DataIntegrityViolationException e) {
        var message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(PENDING_PAYMENT_UNIQUE_INDEX);
    }

    private static PaymentInitializationFailedException alreadyPending(UUID businessPartnerId, ProductType product) {
        return new PaymentInitializationFailedException(
            BusinessExceptionErrorCode.PAYMENT_ALREADY_PENDING,
            "A pending payment for product '%s' already exists for business partner '%s'.".formatted(
                PaymentMapper.toProductTypeDto(product),
                businessPartnerId
            )
        );
    }

    private Payment createPendingPayment(
        UUID businessPartnerId,
        ProductType product,
        Amount amount,
        String baseReturnUrl
    ) {
        if (
            paymentRepository.existsByBusinessPartnerIdAndProductAndStatus(
                businessPartnerId,
                product,
                PaymentStatus.PENDING
            )
        ) {
            throw alreadyPending(businessPartnerId, product);
        }
        var paymentId = UUID.randomUUID();
        var payment = new Payment(
            paymentId,
            businessPartnerId,
            PaymentReferenceIdGenerator.generate(),
            product,
            amount,
            appendPaymentIdPathSegment(baseReturnUrl, paymentId)
        );
        return paymentRepository.saveAndFlush(payment);
    }

    /**
     * Starts a new payment in three steps. Shared by the initial payment and the retry, which only differ
     * in how product, amount and returnUrl are derived.
     * 1. Save the payment in our database with status PENDING, and finish (commit) that database work.
     *    The stored returnUrl is the given baseReturnUrl with the paymentId appended as path segment.
     * 2. Then call Saferpay to open the payment page for it. The browser comes back to that returnUrl.
     * 3. Finally store Saferpay's answer (token, redirect URL, expiration) on the payment, in a second
     *    short transaction.
     *
     * Saferpay is called with no database transaction open on purpose: a slow or hanging Saferpay must
     * not keep a database connection occupied.
     */
    private PaymentInitializationResponseDto startPayment(
        UUID businessPartnerId,
        ProductType product,
        Amount amount,
        String baseReturnUrl
    ) {
        var pending = createPendingPaymentInTransaction(businessPartnerId, product, amount, baseReturnUrl);

        var result = initializeSaferpayPayment(
            pending.getId(),
            new SaferpayInitializePaymentCommand(
                pending.getReferenceId(),
                pending.getAmount().getValue(),
                pending.getAmount().getCurrency(),
                paymentProductCatalog.descriptionFor(pending.getProduct()),
                pending.getReturnUrl()
            )
        );

        var updated = transactionTemplate.execute(status -> applySaferpayResult(pending.getId(), result));
        return PaymentMapper.toResponseDto(updated);
    }

    private static void ensureRetryable(Payment previous) {
        if (previous.getStatus() != PaymentStatus.FAILED && previous.getStatus() != PaymentStatus.CANCELLED) {
            throw new PaymentRetryNotAllowedException(
                "Payment '%s' is in status %s and cannot be retried. Only FAILED or CANCELLED payments can be retried.".formatted(
                    previous.getId(),
                    previous.getStatus()
                )
            );
        }
    }

    /**
     * The stored returnUrl of a payment already ends with the id of that payment (see
     * appendPaymentIdPathSegment). A retry has to end with the id of the new payment instead, so the old
     * segment is removed before the new one is appended. Query string and fragment are kept, e.g.
     * ".../payment/{id}?lang=de" becomes ".../payment?lang=de".
     */
    private static String returnUrlWithoutPaymentId(Payment previous) {
        var stored = previous.getReturnUrl();
        var previousSegment = "/" + previous.getId();
        var queryStart = stored.indexOf('?');
        var fragmentStart = stored.indexOf('#');
        var pathEnd = Math.min(
            queryStart < 0 ? stored.length() : queryStart,
            fragmentStart < 0 ? stored.length() : fragmentStart
        );
        if (!stored.startsWith(previousSegment, pathEnd - previousSegment.length())) {
            throw new IllegalStateException(
                "Return URL '%s' of payment '%s' does not end with the id of that payment.".formatted(
                    stored,
                    previous.getId()
                )
            );
        }
        return stored.substring(0, pathEnd - previousSegment.length()) + stored.substring(pathEnd);
    }

    private Payment applySaferpayResult(UUID paymentId, SaferpayInitializeResult result) {
        var payment = findPayment(paymentId);
        payment.applySaferpayInitialization(result.token(), result.expiration(), result.redirectUrl());
        return payment;
    }

    private Payment markAsFailed(UUID paymentId) {
        var payment = findPayment(paymentId);
        payment.markAsFailed();
        return payment;
    }

    /**
     * Persists the Saferpay transaction ID in a separate transaction (crash-safety).
     * This ensures the transaction ID is saved even if subsequent capture fails.
     */
    private void persistTransactionId(UUID paymentId, String transactionId) {
        transactionTemplateRequiresNew.execute(status -> {
            var payment = findPayment(paymentId);
            payment.setSaferpayTransactionId(transactionId);
            return payment;
        });
    }

    private Payment findPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow(() -> paymentNotFound(paymentId));
    }

    private static ResourceNotFoundException paymentNotFound(UUID paymentId) {
        return new ResourceNotFoundException("No payment found with id " + paymentId);
    }

    /**
     * Builds the URL the browser is sent to after the payment: the caller's returnUrl with the paymentId
     * as last path segment, e.g. ".../payment" becomes ".../payment/{paymentId}". This matches the
     * frontend route pattern "payment/:paymentId".
     */
    private static String appendPaymentIdPathSegment(String returnUrl, UUID paymentId) {
        return UriComponentsBuilder.fromUriString(returnUrl).pathSegment(paymentId.toString()).build().toUriString();
    }

    /**
     * Handles a payment notification from Saferpay (B2B callback).
     * Publishes a TiPaymentChangeNotificationReceivedEvent if the payment exists.
     * Returns 200 even if payment is not found (idempotency - Saferpay may retry).
     *
     * @param referenceId the payment referenceId from Saferpay
     */
    public void handleSaferpayCallback(String referenceId) {
        Optional<Payment> paymentOpt = paymentRepository.findByReferenceId(referenceId);
        if (paymentOpt.isEmpty()) {
            // Return 200 even if not found (idempotency - Saferpay may retry)
            return;
        }
        var payment = paymentOpt.get();

        var event = TiPaymentChangeNotificationReceivedEventBuilder.create().paymentId(payment.getId()).build();

        transactionalOutbox.sendMessage(
            event,
            payment.getId().toString(),
            TiPaymentChangeNotificationReceivedEvent.TypeRef.DEFAULT_TOPIC
        );
    }

    /**
     * Finalizes a payment after receiving a notification from Saferpay.
     * This method is called by PaymentChangeNotificationEventProcessor.
     * It calls Saferpay Assert and Capture, then marks payment as SUCCEEDED.
     *
     * @param paymentId the payment to finalize
     */
    @Transactional
    public void finalizePayment(UUID paymentId) {
        var payment = findPayment(paymentId);

        // Idempotency: if already SUCCEEDED, do nothing
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.info("Payment {} already SUCCEEDED, skipping finalization", paymentId);
            return;
        }

        // Idempotency: if already FAILED or CANCELLED, do not retry
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.CANCELLED) {
            log.info("Payment {} is in terminal state {}, skipping finalization", paymentId, payment.getStatus());
            return;
        }

        var token = payment.getSaferpayToken();
        if (token == null) {
            log.error("Payment {} has no Saferpay token, cannot finalize", paymentId);
            // Persist FAILED state in separate transaction before throwing
            transactionTemplate.execute(status -> markAsFailed(paymentId));
            throw new PaymentFinalizationFailedException(
                "Payment " + paymentId + " has no Saferpay token, cannot finalize"
            );
        }

        // Step 1: Assert - get transaction details from Saferpay
        var assertCommand = new SaferpayAssertCommand(token);
        SaferpayAssertResult assertResult;
        try {
            assertResult = saferpayClient.assertPayment(assertCommand);
        } catch (RuntimeException e) {
            log.error("Saferpay Assert failed for payment {}", paymentId, e);
            // Persist FAILED state in separate transaction before throwing
            transactionTemplateRequiresNew.execute(status -> markAsFailed(paymentId));
            throw new PaymentFinalizationFailedException("Saferpay Assert failed for payment " + paymentId, e);
        }

        var transaction = assertResult.transaction();
        if (transaction == null) {
            log.error("Saferpay Assert returned no transaction for payment {}", paymentId);
            // Persist FAILED state in separate transaction before throwing
            transactionTemplateRequiresNew.execute(status -> markAsFailed(paymentId));
            throw new PaymentFinalizationFailedException(
                "Saferpay Assert returned no transaction for payment " + paymentId
            );
        }

        var transactionId = transaction.transactionId();
        var status = transaction.status();

        // Idempotency: set transaction ID early (crash-safety) - persist in separate transaction
        persistTransactionId(payment.getId(), transactionId);
        // Refresh the payment entity to get the updated version after the separate transaction
        entityManager.refresh(payment);

        // Only proceed with Capture if payment is AUTHORIZED
        if (!SaferpayTransactionStatus.AUTHORIZED.equals(status)) {
            log.warn("Payment {} has status '{}' after Assert, marking as FAILED", paymentId, status);
            payment.markAsFailed();
            return;
        }

        // Validate amount and currency match the expected payment
        validateAmountAndCurrency(payment, transaction, transactionId);

        // Validate liability shift
        validateLiabilityShift(payment, assertResult, transaction, transactionId);

        // Step 2: Capture - confirm the transaction
        var captureCommand = new SaferpayCaptureCommand(new SaferpayTransactionReference(transactionId));
        SaferpayCaptureResult captureResult;
        try {
            captureResult = saferpayClient.captureTransaction(captureCommand);
        } catch (RuntimeException e) {
            log.error("Saferpay Capture failed for payment {}", paymentId, e);
            // Do not mark as FAILED here - capture can be retried
            throw new PaymentFinalizationFailedException("Saferpay Capture failed for payment " + paymentId, e);
        }

        var capturedTransaction = captureResult.transaction();
        if (capturedTransaction == null) {
            log.error("Saferpay Capture returned no transaction for payment {}", paymentId);
            throw new PaymentFinalizationFailedException(
                "Saferpay Capture returned no transaction for payment " + paymentId
            );
        }

        var captureStatus = capturedTransaction.status();
        if (!SaferpayTransactionStatus.CAPTURED.equals(captureStatus)) {
            log.error("Payment {} capture status is '{}', expected CAPTURED", paymentId, captureStatus);
            throw new PaymentFinalizationFailedException(
                "Payment " + paymentId + " capture status is " + captureStatus + ", expected CAPTURED"
            );
        }

        // Step 3: Mark payment as SUCCEEDED with all receipt-relevant data
        var payedAt = Instant.now();
        var paymentMethod =
            transaction.paymentMeans() != null && transaction.paymentMeans().brand() != null
                ? transaction.paymentMeans().brand().paymentMethod()
                : "UNKNOWN";
        var holdername = "Cardholder"; // Saferpay doesn't provide this in Assert response
        var liabilityShift = assertResult.liability() != null && assertResult.liability().liabilityShift();

        payment.markAsSucceeded(transactionId, payedAt, paymentMethod, holdername, liabilityShift);

        // Step 4: Publish TiPaymentSucceededEvent
        var event = TiPaymentSucceededEventBuilder.create().paymentId(paymentId).build();

        transactionalOutbox.sendMessage(event, paymentId.toString(), TiPaymentSucceededEvent.TypeRef.DEFAULT_TOPIC);

        // Step 5: Publish audit record
        var paymentAuditDto = new PaymentAuditDto(
            payment.getId(),
            payment.getVersion(),
            payment.getBusinessPartnerId(),
            AuditMapper.toAuditJson(payment)
        );
        auditPublisher.paymentSucceeded(paymentAuditDto);

        log.info("Payment {} finalized successfully with transactionId {}", paymentId, transactionId);
    }

    /**
     * Validates that the amount and currency from Saferpay Assert match the expected payment amount.
     * If they don't match:
     * - Call Saferpay Transaction/Cancel to release the authorization
     * - Update payment status = FAILED
     * - Save and stop
     *
     * @param payment the payment being finalized
     * @param transaction the transaction from Saferpay Assert
     * @param transactionId the Saferpay transaction ID
     */
    private void validateAmountAndCurrency(Payment payment, SaferpayTransaction transaction, String transactionId) {
        var expectedAmount = payment.getAmount();
        var actualAmount = transaction.amount();
        var actualCurrencyCode = transaction.currencyCode();

        if (actualAmount == null || actualCurrencyCode == null) {
            cancelTransactionAndFail(payment, transactionId, "Saferpay transaction missing amount or currency");
            return;
        }

        // Compare amounts (Saferpay uses minor units, e.g. 15000 = 150.00 CHF)
        if (expectedAmount.getValue() != actualAmount.value()) {
            cancelTransactionAndFail(
                payment,
                transactionId,
                "Amount mismatch: expected " + expectedAmount.getValue() + ", got " + actualAmount.value()
            );
            return;
        }

        // Compare currency codes
        if (!expectedAmount.getCurrency().equalsIgnoreCase(actualCurrencyCode)) {
            cancelTransactionAndFail(
                payment,
                transactionId,
                "Currency mismatch: expected " + expectedAmount.getCurrency() + ", got " + actualCurrencyCode
            );
            return;
        }

        log.debug("Payment {}: Amount and currency validation passed", payment.getId());
    }

    /**
     * Cancels the Saferpay transaction (to release authorization) and marks payment as FAILED.
     * Called when amount/currency validation fails on an AUTHORIZED transaction.
     *
     * @param payment the payment to mark as failed
     * @param transactionId the Saferpay transaction ID
     * @param reason the reason for failure
     */
    private void cancelTransactionAndFail(Payment payment, String transactionId, String reason) {
        log.warn(
            "Payment {}: Validation failed - {}, cancelling Saferpay transaction {}",
            payment.getId(),
            reason,
            transactionId
        );
        try {
            var cancelCommand = new SaferpayCancelCommand(new SaferpayTransactionReference(transactionId));
            saferpayClient.cancelTransaction(cancelCommand);
            log.info("Payment {}: Saferpay transaction {} cancelled successfully", payment.getId(), transactionId);
        } catch (RuntimeException e) {
            log.error("Payment {}: Failed to cancel Saferpay transaction {}", payment.getId(), transactionId, e);
            // Continue to mark as FAILED even if cancel fails
        }
        // Persist FAILED state in separate transaction before throwing
        var paymentId = payment.getId();
        transactionTemplateRequiresNew.execute(status -> {
            var p = findPayment(paymentId);
            p.markAsFailed();
            return p;
        });
        throw new PaymentFinalizationFailedException("Payment " + paymentId + ": " + reason);
    }

    /**
     * Validates liability shift from Saferpay Assert.
     * If liability shift is not achieved (false) and payment method requires 3DS:
     * - Call Saferpay Transaction/Cancel to release the authorization
     * - Update payment status = FAILED
     * - Save payment and stop
     *
     * @param payment the payment being finalized
     * @param assertResult the result from Saferpay Assert
     * @param transaction the transaction from Saferpay Assert (for payment method)
     * @param transactionId the Saferpay transaction ID
     */
    private void validateLiabilityShift(
        Payment payment,
        SaferpayAssertResult assertResult,
        SaferpayTransaction transaction,
        String transactionId
    ) {
        var liability = assertResult.liability();
        var paymentMethod = getPaymentMethod(transaction);

        if (liability == null || !liability.liabilityShift()) {
            log.warn("Payment {}: Liability shift not achieved, paymentMethod={}", payment.getId(), paymentMethod);

            // If payment method requires 3DS (card payments), cancel the transaction to release authorization
            if (requires3DS(paymentMethod)) {
                log.info(
                    "Payment {}: Payment method '{}' requires 3DS, cancelling Saferpay transaction {}",
                    payment.getId(),
                    paymentMethod,
                    transactionId
                );
                try {
                    var cancelCommand = new SaferpayCancelCommand(new SaferpayTransactionReference(transactionId));
                    saferpayClient.cancelTransaction(cancelCommand);
                    log.info(
                        "Payment {}: Saferpay transaction {} cancelled successfully",
                        payment.getId(),
                        transactionId
                    );
                } catch (RuntimeException e) {
                    log.error(
                        "Payment {}: Failed to cancel Saferpay transaction {}",
                        payment.getId(),
                        transactionId,
                        e
                    );
                    // Continue to mark as FAILED even if cancel fails
                }
            } else {
                log.info(
                    "Payment {}: Payment method '{}' does not require 3DS, skipping Saferpay cancel",
                    payment.getId(),
                    paymentMethod
                );
            }

            // Persist FAILED state in separate transaction before throwing
            var paymentId = payment.getId();
            transactionTemplateRequiresNew.execute(status -> {
                var p = findPayment(paymentId);
                p.markAsFailed();
                return p;
            });
            throw new PaymentFinalizationFailedException(
                "Payment " +
                    paymentId +
                    ": Liability shift not achieved (3D-Secure authentication failed or not performed)"
            );
        }
        log.debug("Payment {}: Liability shift validated", payment.getId());
    }

    /**
     * Determines if a payment method requires 3DS (card-based payments).
     * Card payments like VISA, MASTERCARD, AMEX, MAESTRO require 3DS.
     *
     * @param paymentMethod the payment method from Saferpay
     * @return true if 3DS is required
     */
    private boolean requires3DS(String paymentMethod) {
        if (paymentMethod == null) {
            return false;
        }
        var method = paymentMethod.toUpperCase();
        return (
            "VISA".equals(method) ||
            "MASTERCARD".equals(method) ||
            "AMEX".equals(method) ||
            "AMERICAN EXPRESS".equals(method) ||
            "MAESTRO".equals(method) ||
            method.equals("POSTFINANCE") || // PostFinance Card
            "DINERS".equals(method) ||
            "DISCOVER".equals(method) ||
            "JCB".equals(method) ||
            "UNIONPAY".equals(method)
        );
    }

    /**
     * Extracts payment method from transaction.
     */
    private String getPaymentMethod(SaferpayTransaction transaction) {
        return transaction.paymentMeans() != null && transaction.paymentMeans().brand() != null
            ? transaction.paymentMeans().brand().paymentMethod()
            : "UNKNOWN";
    }
}
