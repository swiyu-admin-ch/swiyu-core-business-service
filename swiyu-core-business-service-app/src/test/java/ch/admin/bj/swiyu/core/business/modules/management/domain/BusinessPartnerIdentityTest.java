package ch.admin.bj.swiyu.core.business.modules.management.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Activation, update and deactivation travel on three Kafka topics without an ordering guarantee, so an expiring
 * identity can produce events that overtake each other. These tests pin the version guard.
 */
class BusinessPartnerIdentityTest {

    @Test
    void deactivate_eventOlderThanStoredState_isIgnored() {
        // Given an identity that the TMS already reactivated, published as version 7
        var bpi = identityAtTmsVersion(7L);

        // When the deactivation that preceded it arrives late, carrying version 6
        var deactivated = bpi.deactivate(6L);

        // Then the late event is dropped and the identity stays active
        assertThat(deactivated).isFalse();
        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
        assertThat(bpi.getTmsVersion()).isEqualTo(7L);
    }

    @Test
    void deactivate_eventNewerThanStoredState_isApplied() {
        // Given
        var bpi = identityAtTmsVersion(7L);

        // When the deactivation is the newer event
        var deactivated = bpi.deactivate(8L);

        // Then
        assertThat(deactivated).isTrue();
        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.DEACTIVATED);
        assertThat(bpi.getTmsVersion()).isEqualTo(8L);
    }

    @Test
    void deactivate_sameVersion_isApplied() {
        // Given - a redelivery of the stored state, which has to stay idempotent rather than be dropped
        var bpi = identityAtTmsVersion(7L);

        // When
        var deactivated = bpi.deactivate(7L);

        // Then
        assertThat(deactivated).isTrue();
        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.DEACTIVATED);
    }

    @Test
    void apply_eventOlderThanStoredState_isIgnored() {
        // Given an identity already carrying version 7
        var bpi = identityAtTmsVersion(7L);
        var validUntilBefore = bpi.getValidUntil();

        // When a stale update arrives with version 5
        var applied = apply(bpi, 5L, BusinessPartnerIdentityStatus.DEACTIVATED, Instant.EPOCH);

        // Then nothing of it is taken over
        assertThat(applied).isFalse();
        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
        assertThat(bpi.getValidUntil()).isEqualTo(validUntilBefore);
        assertThat(bpi.getTmsVersion()).isEqualTo(7L);
    }

    @Test
    void apply_storedVersionMissing_isApplied() {
        // Given a row that predates the version tracking - there is nothing to compare against
        var bpi = identityAtTmsVersion(null);

        // When
        var applied = apply(bpi, 3L, BusinessPartnerIdentityStatus.DEACTIVATED, Instant.EPOCH);

        // Then
        assertThat(applied).isTrue();
        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.DEACTIVATED);
        assertThat(bpi.getTmsVersion()).isEqualTo(3L);
    }

    /**
     * The race in the order in which it actually reached the core: the identity expires,
     * the TMS deactivates it (version 6) and immediately reactivates it through the approved renewal
     * (version 7), but the two events arrive the wrong way round.
     */
    @Test
    void outOfOrderDeactivationAfterActivation_leavesTheIdentityActive() {
        // Given the identity as it was before the renewal
        var bpi = identityAtTmsVersion(5L);

        // When the newer activation is processed first
        var activationApplied = apply(bpi, 7L, BusinessPartnerIdentityStatus.ACTIVE, Instant.now());
        // and the older deactivation arrives afterwards
        var deactivationApplied = bpi.deactivate(6L);

        // Then the core agrees with the TMS instead of showing a deactivated partner
        assertThat(activationApplied).isTrue();
        assertThat(deactivationApplied).isFalse();
        assertThat(bpi.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
        assertThat(bpi.getTmsVersion()).isEqualTo(7L);
    }

    private static boolean apply(
        BusinessPartnerIdentity bpi,
        Long tmsVersion,
        BusinessPartnerIdentityStatus status,
        Instant lastActivated
    ) {
        return bpi.apply(
            Instant.parse("2030-01-01T00:00:00Z"),
            List.of("did:example:123"),
            status,
            lastActivated,
            "CHE-123-456-789",
            Map.of("default", "Test Partner"),
            tmsVersion
        );
    }

    private static BusinessPartnerIdentity identityAtTmsVersion(Long tmsVersion) {
        return new BusinessPartnerIdentity(
            UUID.randomUUID(),
            Instant.parse("2029-01-01T00:00:00Z"),
            List.of("did:example:123"),
            BusinessPartnerIdentityStatus.ACTIVE,
            Instant.parse("2026-01-01T00:00:00Z"),
            "CHE-123-456-789",
            Map.of("default", "Test Partner"),
            tmsVersion
        );
    }
}
