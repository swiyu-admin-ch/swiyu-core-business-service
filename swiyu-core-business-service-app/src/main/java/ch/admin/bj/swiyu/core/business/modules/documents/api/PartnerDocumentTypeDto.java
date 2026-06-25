package ch.admin.bj.swiyu.core.business.modules.documents.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PartnerDocumentType", enumAsRef = true)
public enum PartnerDocumentTypeDto {
    TRUST_ONBOARDING_OTHER,
    TRUST_ONBOARDING_DECLARATION_OF_INTENT,
}
