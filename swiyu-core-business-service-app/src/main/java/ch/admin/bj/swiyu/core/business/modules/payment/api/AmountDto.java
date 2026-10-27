package ch.admin.bj.swiyu.core.business.modules.payment.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "Amount")
public record AmountDto(
    @NotNull @Schema(description = "Amount in minor units, e.g. Rappen for CHF.", example = "10000") Long value,
    @NotNull @Schema(description = "ISO 4217 currency code.", example = "CHF") String currency
) {}
