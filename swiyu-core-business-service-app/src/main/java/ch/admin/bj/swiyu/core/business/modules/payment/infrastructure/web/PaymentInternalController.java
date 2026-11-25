package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto;
import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import ch.admin.bj.swiyu.core.business.common.exceptions.PaymentReceiptNotAvailableException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.common.service.mapper.LanguageMapper;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentFilterDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationResponseDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentRetryRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentStatusDto;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentDisabledException;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentReceiptPdfService;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Tag(name = "Payment", description = "Payment API")
@RequestMapping("/api/v2/internal/payment")
class PaymentInternalController {

    private final PaymentService paymentService;
    private final PaymentReceiptPdfService paymentReceiptPdfService;
    private final AuthSupport authSupport;
    private final FunctionalityProperties functionalityProperties;

    @GetMapping
    @PreAuthorize("hasRole('payment','read')")
    @Operation(summary = "Get all payments, newest first by default.")
    @PageableAsQueryParam
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(
        responseCode = "400",
        description = "Invalid filter or pagination, or payment disabled. Possible error codes: DATA_INVALID, PAYMENT_DISABLED.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorized for all of the requested business partners.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @SuppressWarnings("java:S1192") // ignore string literal duplication issue for auth checks
    public PagedModel<PaymentDto> getPayments(
        @Valid @ParameterObject PaymentFilterDto filter,
        @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        ensurePaymentEnabled();
        var authorizedFilter = filter.of(
            authSupport.resolveAuthorizedPartnerIds("payment", "read", filter.businessPartnerIds())
        );
        return new PagedModel<>(paymentService.getPayments(authorizedFilter, pageable));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('payment','read')")
    @PostAuthorize("hasRoleForPartner('payment','read', returnObject.businessPartnerId().toString())")
    @Operation(summary = "Get a single payment by its id.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(
        responseCode = "400",
        description = "Payment disabled. Possible error codes: PAYMENT_DISABLED.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorized.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Payment not found. Possible error codes: RESOURCE_NOT_FOUND.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    public PaymentDto getPayment(@PathVariable UUID paymentId) {
        ensurePaymentEnabled();
        return paymentService.getPayment(paymentId);
    }

    @GetMapping("/business-partner/{businessPartnerId}/payment/{paymentId}/receipt")
    @PreAuthorize("hasRoleForPartner('payment','read', #businessPartnerId.toString())")
    @Operation(summary = "Download a PDF payment receipt for a succeeded payment.")
    @ApiResponse(responseCode = "200", description = "PDF receipt", content = @Content(mediaType = "application/pdf"))
    @ApiResponse(
        responseCode = "404",
        description = "Payment not found. Possible error codes: RESOURCE_NOT_FOUND.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "Payment status is not SUCCEEDED. Possible error codes: PAYMENT_RECEIPT_NOT_AVAILABLE.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorized.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    public void getPaymentReceipt(
        @PathVariable UUID businessPartnerId,
        @PathVariable UUID paymentId,
        @RequestParam(required = false) String locale,
        HttpServletResponse response
    ) throws IOException {
        ensurePaymentEnabled();

        // First, load the payment to get the referenceId for the filename and verify access
        PaymentDto payment = paymentService.getPayment(paymentId);

        if (!payment.businessPartnerId().equals(businessPartnerId)) {
            throw new ResourceNotFoundException("No payment found with id " + paymentId);
        }

        // Only allow receipt generation for SUCCEEDED payments
        if (payment.status() != PaymentStatusDto.SUCCEEDED) {
            throw new PaymentReceiptNotAvailableException(
                "Receipt can only be generated for SUCCEEDED payments. Current status: " + payment.status()
            );
        }

        String filename = "receipt-" + payment.referenceId() + ".pdf";

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString()
        );
        paymentReceiptPdfService.writeReceipt(
            paymentId,
            businessPartnerId,
            LanguageMapper.fromLanguageTag(locale),
            response.getOutputStream()
        );
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRoleForPartner('payment','write', #request.businessPartnerId().toString())")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Initiate a payment for a product via Saferpay.")
    @ApiResponse(responseCode = "201", description = "Payment initiated.")
    @ApiResponse(
        responseCode = "400",
        description = "Business validation failed. Possible error codes: DATA_INVALID, PAYMENT_ALREADY_PENDING, PAYMENT_DISABLED, PAYMENT_RETURN_URL_NOT_ALLOWED, BUSINESS_DATA_INTEGRITY_VIOLATION.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorized.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    public PaymentInitializationResponseDto initiatePayment(
        @Valid @RequestBody PaymentInitializationRequestDto request
    ) {
        ensurePaymentEnabled();
        return paymentService.initiatePayment(request);
    }

    @PostMapping("/retry")
    @PreAuthorize("hasRoleForPartner('payment','write', #request.businessPartnerId().toString())")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Retry a failed or cancelled payment.",
        description = "Creates a new payment with the same product and amount as the previous one and returns the URL of a new Saferpay payment page."
    )
    @ApiResponse(responseCode = "201", description = "Retry started, a new payment has been created.")
    @ApiResponse(
        responseCode = "400",
        description = "Business validation failed. Possible error codes: DATA_INVALID, PAYMENT_ALREADY_PENDING, PAYMENT_DISABLED, BUSINESS_DATA_INTEGRITY_VIOLATION.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorized.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "No payment with this id exists for the business partner. Possible error codes: RESOURCE_NOT_FOUND.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "The payment is neither FAILED nor CANCELLED, so it cannot be retried. Possible error codes: PAYMENT_NOT_RETRYABLE.",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorDto.class))
    )
    public PaymentInitializationResponseDto retryPayment(@Valid @RequestBody PaymentRetryRequestDto request) {
        ensurePaymentEnabled();
        return paymentService.retryPayment(request.previousPaymentId(), request.businessPartnerId());
    }

    private void ensurePaymentEnabled() {
        if (!Boolean.TRUE.equals(functionalityProperties.paymentEnabled())) {
            throw new PaymentDisabledException();
        }
    }
}
