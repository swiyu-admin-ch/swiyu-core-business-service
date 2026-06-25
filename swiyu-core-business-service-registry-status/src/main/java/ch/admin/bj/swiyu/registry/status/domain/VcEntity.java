/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A VcEntity represents a VC of different formats and types in our data store.
 * <p>
 * VcEntity does store the actual vc (with signature and other addenda)
 * along the payload json.
 * This allows to filter and search for payload driven elements specific to the
 * type of VC handled while also providing a generalized way how to handle VCs of
 * different types and formats.
 */
@Entity
@NoArgsConstructor // JPA
@Getter
@Table(name = "vc_entity")
@EntityListeners(AuditingEntityListener.class)
public class VcEntity {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_id", referencedColumnName = "id")
    private StatusListDatastoreEntity base;

    @Column(name = "vc_type")
    @Enumerated(EnumType.STRING)
    private VcType vcType;

    @Column(name = "raw_vc")
    private String rawVc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vc_payload")
    private String vcPayload;

    @Column(name = "read_uri")
    private String readUri;

    public VcEntity(StatusListDatastoreEntity base, VcType vcType, String rawVc, String vcPayload, String readUri) {
        this.base = base;
        this.vcType = vcType;
        this.rawVc = rawVc;
        this.vcPayload = vcPayload;
        this.readUri = readUri;
    }

    public void updateVc(String rawVc, String vcPayload) {
        this.vcPayload = vcPayload;
        this.rawVc = rawVc;
    }
}
