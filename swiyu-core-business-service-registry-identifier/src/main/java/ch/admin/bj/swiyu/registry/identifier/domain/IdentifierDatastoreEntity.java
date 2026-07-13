/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(name = "datastore_entity")
@NoArgsConstructor // JPA required
public class IdentifierDatastoreEntity {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    private UUID id;

    @Version
    @Column(name = "version")
    private int version;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DatastoreStatus status;

    public IdentifierDatastoreEntity(UUID id) {
        this.id = id;
        this.status = DatastoreStatus.SETUP;
    }

    public void changeStatus(DatastoreStatus status) {
        this.status = status;
    }
}
