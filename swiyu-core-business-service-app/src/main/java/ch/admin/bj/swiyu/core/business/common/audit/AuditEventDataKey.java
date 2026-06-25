package ch.admin.bj.swiyu.core.business.common.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuditEventDataKey {
    USE_CASE_CATEGORY_ID("useCaseCategoryId"),
    BUSINESS_PARTNER_ID("businessPartnerId");

    private final String key;
}
