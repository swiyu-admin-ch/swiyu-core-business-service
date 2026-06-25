package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VqpsSubmissionStatus", enumAsRef = true)
public enum VqpsSubmissionStatusDto {
    ACCEPTED,
    PUBLICATION_SUCCEEDED,
    PUBLICATION_FAILED,
}
