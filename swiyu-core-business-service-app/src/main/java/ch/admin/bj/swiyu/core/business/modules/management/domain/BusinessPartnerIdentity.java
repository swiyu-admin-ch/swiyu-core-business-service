package ch.admin.bj.swiyu.core.business.modules.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Trust identity of a business partner, owned by the Trust Management Service and only changed through
 * TMS BPI events. Keyed by the id of the business partner it belongs to - it has no id of its own.
 *
 * <p>Kept in its own table so that identity changes never touch (and never version-bump) the
 * {@link BusinessEntity} row, which maps this entity read-only.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessPartnerIdentity {

    @Id
    private UUID businessPartnerId;

    private Instant validUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> trustedIdentifier;

    @Enumerated(EnumType.STRING)
    @NotNull
    private BusinessPartnerIdentityStatus status;

    private Instant lastActivated;

    private String uid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> entityName;

    /**
     * Version as published by the Trust Management Service.
     * Used to detect stale / out-of-order events.
     */
    private Long tmsVersion;

    @Version
    @NotNull
    private Long version;

    @SuppressWarnings("java:S107") // mirrors the fields of the identity
    public BusinessPartnerIdentity(
        UUID businessPartnerId,
        Instant validUntil,
        List<String> trustedIdentifier,
        BusinessPartnerIdentityStatus status,
        Instant lastActivated,
        String uid,
        Map<String, String> entityName,
        Long tmsVersion
    ) {
        this.businessPartnerId = businessPartnerId;
        this.validUntil = validUntil;
        this.trustedIdentifier = trustedIdentifier;
        this.status = status;
        this.lastActivated = lastActivated;
        this.uid = uid;
        this.entityName = entityName;
        this.tmsVersion = tmsVersion;
    }

    /**
     * Overwrites all identity data with the data of a TMS BPI activated or updated event.
     */
    public void apply(
        Instant validUntil,
        List<String> trustedIdentifier,
        BusinessPartnerIdentityStatus status,
        Instant lastActivated,
        String uid,
        Map<String, String> entityName,
        Long tmsVersion
    ) {
        this.validUntil = validUntil;
        this.trustedIdentifier = trustedIdentifier;
        this.status = status;
        this.lastActivated = lastActivated;
        this.uid = uid;
        this.entityName = entityName;
        this.tmsVersion = tmsVersion;
    }

    /**
     * Sets the status to DEACTIVATED and updates the TMS version, preserving all other data.
     * Used when processing a TiBusinessPartnerIdentityDeactivatedEvent.
     */
    public void deactivate(long tmsVersion) {
        this.status = BusinessPartnerIdentityStatus.DEACTIVATED;
        this.tmsVersion = tmsVersion;
    }
}
