package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Tag(name = "Saferpay Notify B2B", description = "Saferpay payment notification callback")
@RequestMapping("/api/v1/saferpay")
class SaferpayNotifyB2BController {

    private final PaymentService paymentService;

    @GetMapping("/notify")
    @Operation(summary = "Handle Saferpay payment notification callback")
    public void handleSaferpayCallback(
        @Parameter(description = "Saferpay orderId / our payment referenceId") @RequestParam @NotNull String referenceId
    ) {
        paymentService.handleSaferpayCallback(referenceId);
    }
}
