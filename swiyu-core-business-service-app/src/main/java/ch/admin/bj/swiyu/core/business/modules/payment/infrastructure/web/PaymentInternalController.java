package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto;
import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentFilterDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationResponseDto;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentDisabledException;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Tag(name = "Payment", description = "Payment API")
@RequestMapping("/api/v2/internal/payment")
class PaymentInternalController {

    private final PaymentService paymentService;
    private final AuthSupport authSupport;
    private final FeaturesProperties featuresProperties;

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

    private void ensurePaymentEnabled() {
        if (!Boolean.TRUE.equals(featuresProperties.getEidartfe1297Payment())) {
            throw new PaymentDisabledException();
        }
    }
}
