package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SigningRule", enumAsRef = true)
public enum SigningRuleDto {
    SINGLE_SIGNATURE,
    JOINT_SIGNATURE_TWO,
    JOINT_SIGNATURE_THREE,
}
