package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProtectedVerificationSubmissionStatus", enumAsRef = true)
public enum ProtectedVerificationSubmissionStatusDto {
    SUBMITTED,
    APPROVED,
    REJECTED,
}
