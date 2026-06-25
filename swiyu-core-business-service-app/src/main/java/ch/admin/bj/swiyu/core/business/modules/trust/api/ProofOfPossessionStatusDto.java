package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProofOfPossessionStatus", enumAsRef = true)
public enum ProofOfPossessionStatusDto {
    VALID,
    NOT_SUPPLIED,
}
