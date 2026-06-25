package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VcSchemaSubmissionStatus", enumAsRef = true)
public enum VcSchemaSubmissionStatusDto {
    ACCEPTED,
    SUCCEEDED,
    FAILED,
}
