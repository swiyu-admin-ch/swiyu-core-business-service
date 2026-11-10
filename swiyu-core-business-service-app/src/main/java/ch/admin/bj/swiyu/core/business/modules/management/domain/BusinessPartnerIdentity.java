package ch.admin.bj.swiyu.core.business.modules.management.domain;

import static ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus.ACTIVE;

import jakarta.persistence.*;
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
     * Overwrites all identity data with an activated or updated event, unless the event is older than what is stored.
     *
     * @return true if the data was applied, false if the event was stale and therefore ignored
     */
    public boolean apply(
        Instant validUntil,
        List<String> trustedIdentifier,
        BusinessPartnerIdentityStatus status,
        Instant lastActivated,
        String uid,
        Map<String, String> entityName,
        Long tmsVersion
    ) {
        if (isStale(tmsVersion)) {
            return false;
        }
        this.validUntil = validUntil;
        this.trustedIdentifier = trustedIdentifier;
        this.status = status;
        this.lastActivated = lastActivated;
        this.uid = uid;
        this.entityName = entityName;
        this.tmsVersion = tmsVersion;
        return true;
    }

    /**
     * Sets the status to DEACTIVATED and updates the TMS version, preserving all other data, unless the
     * event is older than what is already stored.
     * Used when processing a TiBusinessPartnerIdentityDeactivatedEvent.
     *
     * @return true if the identity was deactivated, false if the event was stale and therefore ignored
     */
    public boolean deactivate(long tmsVersion) {
        if (isStale(tmsVersion)) {
            return false;
        }
        this.status = BusinessPartnerIdentityStatus.DEACTIVATED;
        this.tmsVersion = tmsVersion;
        return true;
    }

    /**
     * Activation, update and deactivation travel on three Kafka topics without an ordering guarantee, so events can
     * overtake each other. Only strictly older ones are dropped: the same version is a redelivery and idempotent, and
     * a missing stored version predates the tracking.
     */
    private boolean isStale(Long incomingTmsVersion) {
        return this.tmsVersion != null && incomingTmsVersion != null && incomingTmsVersion < this.tmsVersion;
    }

    public boolean isActive() {
        return this.getStatus() == ACTIVE;
    }
}
