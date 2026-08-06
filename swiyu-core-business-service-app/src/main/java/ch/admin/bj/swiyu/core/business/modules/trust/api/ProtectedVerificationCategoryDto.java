package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProtectedVerificationCategory", enumAsRef = true)
public enum ProtectedVerificationCategoryDto {
    PERSONAL_ADMINISTRATIVE_NUMBER,
}
