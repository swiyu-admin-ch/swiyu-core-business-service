package ch.admin.bj.swiyu.core.business.modules.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BusinessPartnerIdentity {

    private Instant validUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> trustedIdentifier;

    @Enumerated(EnumType.STRING)
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

    /**
     * Returns a copy of this identity with status set to DEACTIVATED and the TMS version updated.
     * Used when processing a TiBusinessPartnerIdentityDeactivatedEvent.
     */
    public BusinessPartnerIdentity withDeactivated(long tmsVersion) {
        return BusinessPartnerIdentity.builder()
            .validUntil(this.validUntil)
            .trustedIdentifier(this.trustedIdentifier)
            .status(BusinessPartnerIdentityStatus.DEACTIVATED)
            .lastActivated(this.lastActivated)
            .uid(this.uid)
            .entityName(this.entityName)
            .tmsVersion(tmsVersion)
            .build();
    }
}
