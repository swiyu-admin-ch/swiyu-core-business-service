package ch.admin.bj.swiyu.core.business.modules.payment.service;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationResponseDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentProductCatalog;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentReferenceIdGenerator;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentRepository;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentReturnUrlValidator;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializePaymentCommand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializeResult;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentInitializationFailedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PaymentService {

    /** Partial unique index of the payment table: at most one PENDING payment per business partner and product. */
    private static final String PENDING_PAYMENT_UNIQUE_INDEX = "payment_business_partner_id_product_pending_key";

    private final PaymentRepository paymentRepository;
    private final PaymentReturnUrlValidator paymentReturnUrlValidator;
    private final PaymentProductCatalog paymentProductCatalog;
    private final FeaturesProperties featuresProperties;
    private final BusinessPartnerService businessPartnerService;
    private final SaferpayClient saferpayClient;
    private final TransactionTemplate transactionTemplate;

    /**
     * Initiates a payment in three steps:
     * 1. Save the payment in our database with status PENDING, and finish (commit) that database work.
     *    The stored returnUrl is the caller's returnUrl with the paymentId appended as path segment.
     * 2. Then call Saferpay to open the payment page for it. The browser comes back to that returnUrl.
     * 3. Finally store Saferpay's answer (token, redirect URL, expiration) on the payment, in a second
     *    short transaction.
     *
     * Saferpay is called with no database transaction open on purpose: a slow or hanging Saferpay must
     * not keep a database connection occupied.
     */
    public PaymentInitializationResponseDto initiatePayment(PaymentInitializationRequestDto request) {
        if (!Boolean.TRUE.equals(featuresProperties.getEidartfe1297Payment())) {
            throw new PaymentInitializationFailedException(
                BusinessExceptionErrorCode.PAYMENT_DISABLED,
                "Payment feature is disabled."
            );
        }
        // the returnUrl is supplied by the caller - only accept it if it points to one of our own frontends
        paymentReturnUrlValidator.validate(request.returnUrl());
        businessPartnerService.validateBusinessPartnerExists(request.businessPartnerId());

        var pending = createPendingPaymentInTransaction(request);

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
    private Payment createPendingPaymentInTransaction(PaymentInitializationRequestDto request) {
        try {
            return transactionTemplate.execute(status -> createPendingPayment(request));
        } catch (DataIntegrityViolationException e) {
            if (isPendingPaymentIndexViolation(e)) {
                throw alreadyPending(request);
            }
            // Anything else is unexpected, e.g. the (very unlikely) collision of the random referenceId.
            throw e;
        }
    }

    private static boolean isPendingPaymentIndexViolation(DataIntegrityViolationException e) {
        var message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(PENDING_PAYMENT_UNIQUE_INDEX);
    }

    private static PaymentInitializationFailedException alreadyPending(PaymentInitializationRequestDto request) {
        return new PaymentInitializationFailedException(
            BusinessExceptionErrorCode.PAYMENT_ALREADY_PENDING,
            "A pending payment for product '%s' already exists for business partner '%s'.".formatted(
                request.product(),
                request.businessPartnerId()
            )
        );
    }

    private Payment createPendingPayment(PaymentInitializationRequestDto request) {
        var product = PaymentMapper.toProductType(request.product());
        if (
            paymentRepository.existsByBusinessPartnerIdAndProductAndStatus(
                request.businessPartnerId(),
                product,
                PaymentStatus.PENDING
            )
        ) {
            throw alreadyPending(request);
        }
        var amount = Amount.chf(paymentProductCatalog.priceFor(product));
        var paymentId = UUID.randomUUID();
        var payment = new Payment(
            paymentId,
            request.businessPartnerId(),
            PaymentReferenceIdGenerator.generate(),
            product,
            amount,
            appendPaymentIdPathSegment(request.returnUrl().toString(), paymentId)
        );
        return paymentRepository.saveAndFlush(payment);
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
}
