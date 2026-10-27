package ch.admin.bj.swiyu.core.business.modules.payment.api;

import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "Payment")
public record PaymentDto(
    @NotNull UUID id,
    @NotNull @Schema(description = "Id of the business partner the payment is for.") UUID businessPartnerId,
    @NotNull
    @Schema(description = "Human-readable id of the payment, also known to Saferpay.", example = "P-A3F8B2C1")
    String referenceId,
    @NotNull PaymentStatusDto status,
    @NotNull ProductTypeDto product,
    @NotNull
    @Schema(description = "URL the browser is redirected back to after the payment, incl. the id of the payment.")
    String returnUrl,
    @Schema(description = "When the payment was completed. Only set for succeeded payments.") Instant payedAt,
    @NotNull AmountDto amount,
    @NotNull Instant createdAt,
    @NotNull Instant updatedAt
) implements ListItemDto {}
