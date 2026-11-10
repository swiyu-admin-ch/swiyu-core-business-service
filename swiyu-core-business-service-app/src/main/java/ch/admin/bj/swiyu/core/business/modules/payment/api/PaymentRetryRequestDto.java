package ch.admin.bj.swiyu.core.business.modules.payment.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "PaymentRetryRequest")
public record PaymentRetryRequestDto(
    @NotNull
    @Schema(description = "Id of the payment that failed or was cancelled and shall be paid again.")
    UUID previousPaymentId,
    @NotNull @Schema(description = "Id of the business partner the previous payment belongs to.") UUID businessPartnerId
) {}
