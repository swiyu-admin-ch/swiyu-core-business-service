package ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Representation of a VcSchemaSubmission. Only governmental institutions can submit these.
 */
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class VcSchemaSubmission {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotNull
    UUID partnerId;

    @Version
    @NotNull
    Long version;

    @NotNull
    VcSchemaSubmissionStatus status;

    @NotNull
    String file;

    String failureReason;

    public VcSchemaSubmission(UUID id, UUID partnerId, String file) {
        this.id = id;
        this.partnerId = partnerId;
        this.file = file;
        this.status = VcSchemaSubmissionStatus.ACCEPTED;
        this.version = 1L;
    }

    public VcSchemaSubmission(UUID partnerId, String file) {
        this.partnerId = partnerId;
        this.file = file;
        this.status = VcSchemaSubmissionStatus.ACCEPTED;
    }

    protected VcSchemaSubmission() {
        // JPA
    }

    public void markAsSucceeded() {
        this.status = VcSchemaSubmissionStatus.SUCCEEDED;
    }

    public void markAsFailed(String reason) {
        this.status = VcSchemaSubmissionStatus.FAILED;
        this.failureReason = reason;
    }
}
