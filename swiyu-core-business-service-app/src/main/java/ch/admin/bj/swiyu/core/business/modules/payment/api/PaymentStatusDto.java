package ch.admin.bj.swiyu.core.business.modules.payment.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaymentStatus", enumAsRef = true)
public enum PaymentStatusDto {
    PENDING,
    FAILED,
    CANCELLED,
    SUCCEEDED,
}
