package ch.admin.bj.swiyu.core.business.modules.payment.api;

import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.net.URL;
import java.util.UUID;

@Schema(name = "PaymentInitializationRequest")
public record PaymentInitializationRequestDto(
    @NotNull @Schema(description = "Id of the business partner the payment is for.") UUID businessPartnerId,
    @NotNull @Schema(description = "Product to purchase.") ProductTypeDto product,
    @NotNull
    @Schema(
        description = "URL the browser is redirected back to after the payment. The id of the payment is appended as last path segment (.../{paymentId}). Must match one of the configured allowed origins.",
        example = "https://portal.trust-infra.swiyu.admin.ch/ui/payment"
    )
    URL returnUrl
) {}
