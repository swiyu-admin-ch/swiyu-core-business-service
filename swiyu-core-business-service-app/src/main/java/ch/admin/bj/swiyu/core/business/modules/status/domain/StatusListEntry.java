package ch.admin.bj.swiyu.core.business.modules.status.domain;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Representation of our link from out business entity to the status registry
 */
@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StatusListEntry {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    UUID statusRegistryEntryId;

    @NotNull
    UUID businessEntityId;

    /**
     * Upload count is currently only used to artifically trigger the audit mechanisms during the upload
     * of a status list as this process actually does not require an update on the DB entity but should be tracked
     * through the audit system.
     */
    int uploadCount;

    public StatusListEntry(UUID statusRegistryEntryId, UUID businessEntityId) {
        this.statusRegistryEntryId = statusRegistryEntryId;
        this.businessEntityId = businessEntityId;
        this.uploadCount = 0;
    }

    public int increaseUploadCount() {
        return ++uploadCount;
    }
}
