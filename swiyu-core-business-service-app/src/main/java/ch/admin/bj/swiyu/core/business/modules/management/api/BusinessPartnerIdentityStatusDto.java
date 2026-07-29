package ch.admin.bj.swiyu.core.business.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BusinessPartnerIdentityStatus", enumAsRef = true)
public enum BusinessPartnerIdentityStatusDto {
    ACTIVE,
    DEACTIVATED,
}
