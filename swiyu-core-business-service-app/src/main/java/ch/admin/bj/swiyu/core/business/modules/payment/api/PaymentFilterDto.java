package ch.admin.bj.swiyu.core.business.modules.payment.api;

import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;

@Schema(name = "PaymentFilter")
public record PaymentFilterDto(
    @RequestParam(required = false) List<UUID> businessPartnerIds,
    @RequestParam(required = false) List<ProductTypeDto> productTypes,
    @RequestParam(required = false) List<PaymentStatusDto> statuses
) {
    public PaymentFilterDto of(List<UUID> businessPartnerIds) {
        return new PaymentFilterDto(businessPartnerIds, productTypes, statuses);
    }
}
