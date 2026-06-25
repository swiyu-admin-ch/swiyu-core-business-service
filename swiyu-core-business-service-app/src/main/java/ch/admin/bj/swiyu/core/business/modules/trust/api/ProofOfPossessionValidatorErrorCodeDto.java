package ch.admin.bj.swiyu.core.business.modules.trust.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProofOfPossessionValidatorErrorCode", enumAsRef = true)
public enum ProofOfPossessionValidatorErrorCodeDto {
    MISSING_NONCE("missing_nonce"),
    INVALID_NONCE_FORMAT("invalid_nonce_format"),
    MISMATCHING_NONCE("mismatching_nonce"),
    INVALID_PAYLOAD("invalid_payload"),
    INVALID_CRYPTO_INTEGRITY("invalid_crypto_integrity"),
    INVALID_DID("invalid_did"),
    PROOF_OF_POSSESSION_TOO_OLD("proof_of_possession_too_old"),
    INVALID_JWT("invalid_jwt");

    private final String code;

    ProofOfPossessionValidatorErrorCodeDto(String code) {
        this.code = code;
    }

    @JsonValue
    @Override
    public String toString() {
        return code;
    }
}
