package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingSubmissionDocumentType", enumAsRef = true)
public enum TrustOnboardingSubmissionDocumentTypeDto {
    TRUST_ONBOARDING_OTHER,
    TRUST_ONBOARDING_DECLARATION_OF_INTENT,
}
