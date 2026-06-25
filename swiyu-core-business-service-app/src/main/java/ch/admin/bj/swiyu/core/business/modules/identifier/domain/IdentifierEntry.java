package ch.admin.bj.swiyu.core.business.modules.identifier.domain;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class IdentifierEntry {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    private UUID id;

    @NotNull
    private UUID businessEntityId;

    private int uploadCount;

    private String did;

    @Setter
    private String description;

    @Enumerated(EnumType.STRING)
    private IdentifierStatus status;

    public IdentifierEntry(UUID id, UUID businessEntityId) {
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.uploadCount = 0;
        this.status = IdentifierStatus.NOT_INITIALIZED;
    }

    public void updateDidAndActivate(String did) {
        this.status = IdentifierStatus.INITIALIZED;
        this.did = did;
        this.increaseUploadCount();
    }

    public void updateDidAndDeactivate(String did) {
        this.status = IdentifierStatus.USER_DEACTIVATED;
        this.did = did;
        this.increaseUploadCount();
    }

    public void deactivateDueToUnsupportedFormat() {
        this.status = IdentifierStatus.DEACTIVATED_BY_MIGRATION_BECAUSE_OF_UNSUPPORTED_FORMAT;
    }

    /**
     * Temp. method to set a did after migration. Will be removed, do not use for new business cases.
     */
    public void setDidAfterMigration(String did) {
        this.did = did;
    }

    private void increaseUploadCount() {
        this.uploadCount++;
    }
}
