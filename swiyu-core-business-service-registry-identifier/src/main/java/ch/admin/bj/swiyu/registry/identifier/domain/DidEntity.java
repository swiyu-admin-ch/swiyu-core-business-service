/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
@EntityListeners(AuditingEntityListener.class)
@Table(name = "did_entity")
public class DidEntity {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Version
    @Column(name = "version")
    private int version;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_id", referencedColumnName = "id")
    private IdentifierDatastoreEntity base;

    @Column(name = "file_type")
    @Enumerated(EnumType.STRING)
    private DidType fileType;

    @Column(name = "content")
    private String content;

    @Column(name = "read_uri")
    private String readUri;

    public DidEntity(IdentifierDatastoreEntity base, DidType fileType, String content, String readUri) {
        this.base = base;
        this.fileType = fileType;
        this.content = content;
        this.readUri = readUri;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
