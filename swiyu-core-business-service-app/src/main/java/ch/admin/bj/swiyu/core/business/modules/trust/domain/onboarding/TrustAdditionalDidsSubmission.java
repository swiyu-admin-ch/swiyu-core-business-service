package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class TrustAdditionalDidsSubmission {

    @Id
    private UUID id;

    @NotNull
    private UUID partnerId;

    @Version
    @NotNull
    private Long version;

    @Embedded
    @Valid
    @NotNull
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull
    private ProofOfPossession permissionDid;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull
    private List<ProofOfPossession> didsToAdd;

    @Enumerated(EnumType.STRING)
    private TrustAdditionalDidsRejectReason rejectReason;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TrustAdditionalDidsSubmissionStatus status;

    protected TrustAdditionalDidsSubmission() {
        // JPA
    }

    public TrustAdditionalDidsSubmission(
        UUID partnerId,
        ProofOfPossession permissionDid,
        List<ProofOfPossession> didsToAdd
    ) {
        this.id = UUID.randomUUID();
        this.partnerId = partnerId;
        this.permissionDid = permissionDid;
        this.didsToAdd = didsToAdd;
        this.status = TrustAdditionalDidsSubmissionStatus.UNSUBMITTED;
    }

    public void markAsValidatedAndSubmitted() {
        this.permissionDid = this.permissionDid.toValid();
        this.didsToAdd = this.didsToAdd.stream().map(ProofOfPossession::toValid).toList();
        this.status = TrustAdditionalDidsSubmissionStatus.SUBMITTED;
    }

    public void markAsPublished() {
        this.status = TrustAdditionalDidsSubmissionStatus.SUCCEEDED;
    }

    public void markAsFailed(TrustAdditionalDidsRejectReason reason) {
        this.status = TrustAdditionalDidsSubmissionStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void refreshNonces() {
        var newNonce = UUID.randomUUID().toString();
        this.permissionDid = new ProofOfPossession(this.permissionDid.getDid(), newNonce);
        this.didsToAdd = this.didsToAdd.stream()
            .map(pop -> new ProofOfPossession(pop.getDid(), newNonce))
            .toList();
    }
}
