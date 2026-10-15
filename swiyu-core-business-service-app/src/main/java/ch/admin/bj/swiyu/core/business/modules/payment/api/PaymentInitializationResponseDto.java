package ch.admin.bj.swiyu.core.business.modules.payment.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URL;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(name = "PaymentInitializationResponse")
public record PaymentInitializationResponseDto(
    @Schema(description = "Id of the newly created payment.") UUID paymentId,
    @Schema(description = "Id of the business partner the payment is for.") UUID businessPartnerId,
    @Schema(description = "URL to open in the browser to continue the payment on Saferpay's hosted payment page.")
    URL redirectUrl
) {}
