package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonAutoDetect(
    // serialize only fields to json, no getters/setters
    fieldVisibility = ANY,
    setterVisibility = NONE,
    getterVisibility = NONE,
    isGetterVisibility = NONE,
    creatorVisibility = NONE
)
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class ProofOfPossession {

    @NotNull
    private ProofOfPossessionStatus status;

    @NotNull
    private String nonce;

    @NotNull
    private String did;

    @Nullable
    private Instant verifiedAt;

    public ProofOfPossession(String did, String nonce) {
        this.did = did;
        this.status = ProofOfPossessionStatus.NOT_SUPPLIED;
        this.nonce = nonce;
    }

    private ProofOfPossession(String did, ProofOfPossessionStatus status, String nonce, Instant verifiedAt) {
        this.did = did;
        this.status = status;
        this.verifiedAt = verifiedAt;
        this.nonce = nonce;
    }

    public ProofOfPossession toValid() {
        return new ProofOfPossession(this.did, ProofOfPossessionStatus.VALID, this.nonce, Instant.now());
    }

    public ProofOfPossession toNotSupplied() {
        return new ProofOfPossession(
            this.did,
            ProofOfPossessionStatus.NOT_SUPPLIED,
            UUID.randomUUID().toString(),
            null
        );
    }
}
