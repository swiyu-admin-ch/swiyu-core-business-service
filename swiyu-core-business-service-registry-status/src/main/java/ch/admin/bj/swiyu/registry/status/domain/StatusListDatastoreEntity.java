/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A DatastoreEntity is the generic anchor for different files which are managed by this datastore.
 * <p>
 * It allows for unified handling of the most common management actions which we want to perform on our stored data.
 * For example: Deleting or deactivation of entries.
 */
@Entity
@Getter
@Table(name = "datastore_entity")
@EntityListeners(AuditingEntityListener.class)
@Slf4j
public class StatusListDatastoreEntity {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DatastoreStatus status;

    public StatusListDatastoreEntity() {
        this.status = DatastoreStatus.SETUP;
    }

    public void changeStatus(DatastoreStatus newStatus) {
        log.info("changing status of datastore entity {} from {} to {}", id, this.status, newStatus);
        this.status = newStatus;
    }

    public void activate() {
        changeStatus(DatastoreStatus.ACTIVE);
    }
}
