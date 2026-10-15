package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationResponseDto;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return paymentService.initiatePayment(request);
    }
}
