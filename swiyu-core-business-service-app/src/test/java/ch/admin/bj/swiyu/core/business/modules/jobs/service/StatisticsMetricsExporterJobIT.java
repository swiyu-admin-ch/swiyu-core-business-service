package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeBusiness;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeGov;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.IdentifierStatusDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntry;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@SpringBootTest
@WithJeapAuthenticationToken(username = "Test")
@WithAllTestContainerInitializers
@TestPropertySource(properties = "app.jobs.statistics.enabled=true")
class StatisticsMetricsExporterJobIT {

    @Autowired
    private StatisticsMetricsExporterJob statisticsMetricsExporterJob;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TestRepositories testRepositories;

    private static IdentifierEntry initialized(UUID id, UUID partnerId) {
        var entry = new IdentifierEntry(id, partnerId);
        entry.updateDidAndActivate("did:tdw:" + id);
        return entry;
    }

    private static IdentifierEntry deactivated(UUID id, UUID partnerId) {
        var entry = new IdentifierEntry(id, partnerId);
        entry.updateDidAndDeactivate("did:tdw:" + id);
        return entry;
    }

    @BeforeEach
    void setUp() {
        testRepositories.truncateTables();
    }

    @Test
    void refreshStatistics_updatesCountersFromRepositories() {
        // given
        var businessEntityIds = java.util.List.of(
            testRepositories.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID())).getId(),
            testRepositories.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID())).getId(),
            testRepositories.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID())).getId()
        );

        for (var i = 0; i < 7; i++) {
            testRepositories.statusListEntry.save(
                new StatusListEntry(UUID.randomUUID(), businessEntityIds.get(i % businessEntityIds.size()))
            );
        }

        testRepositories.identifierEntry.save(new IdentifierEntry(UUID.randomUUID(), businessEntityIds.get(0)));
        testRepositories.identifierEntry.save(new IdentifierEntry(UUID.randomUUID(), businessEntityIds.get(1)));
        testRepositories.identifierEntry.save(new IdentifierEntry(UUID.randomUUID(), businessEntityIds.get(2)));
        testRepositories.identifierEntry.save(initialized(UUID.randomUUID(), businessEntityIds.get(0)));
        testRepositories.identifierEntry.save(initialized(UUID.randomUUID(), businessEntityIds.get(1)));
        testRepositories.identifierEntry.save(deactivated(UUID.randomUUID(), businessEntityIds.get(2)));

        // when
        statisticsMetricsExporterJob.refreshStatistics();

        // then
        assertThat(meterRegistry.get("swiyu_cbs_business_partner").gauge().value()).isEqualTo(3.0);
        assertThat(
            meterRegistry
                .get("swiyu_cbs_business_partner_by_type")
                .tag("type", BusinessPartnerTypeDto.BUSINESS.name())
                .gauge()
                .value()
        ).isEqualTo(2.0);
        assertThat(
            meterRegistry
                .get("swiyu_cbs_business_partner_by_type")
                .tag("type", BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION.name())
                .gauge()
                .value()
        ).isEqualTo(1.0);
        assertThat(
            meterRegistry
                .get("swiyu_cbs_business_partner_by_type")
                .tag("type", BusinessPartnerTypeDto.INDIVIDUAL.name())
                .gauge()
                .value()
        ).isEqualTo(0.0);

        assertThat(meterRegistry.get("swiyu_cbs_status_list").gauge().value()).isEqualTo(7.0);

        assertThat(meterRegistry.get("swiyu_cbs_identifier").gauge().value()).isEqualTo(6.0);
        assertThat(
            meterRegistry
                .get("swiyu_cbs_identifier_by_status")
                .tag("status", IdentifierStatusDto.NOT_INITIALIZED.name())
                .gauge()
                .value()
        ).isEqualTo(3.0);
        assertThat(
            meterRegistry
                .get("swiyu_cbs_identifier_by_status")
                .tag("status", IdentifierStatusDto.INITIALIZED.name())
                .gauge()
                .value()
        ).isEqualTo(2.0);
        assertThat(
            meterRegistry
                .get("swiyu_cbs_identifier_by_status")
                .tag("status", IdentifierStatusDto.USER_DEACTIVATED.name())
                .gauge()
                .value()
        ).isEqualTo(1.0);
        assertThat(
            meterRegistry
                .get("swiyu_cbs_identifier_by_status")
                .tag("status", IdentifierStatusDto.DEACTIVATED_BY_MIGRATION_BECAUSE_OF_UNSUPPORTED_FORMAT.name())
                .gauge()
                .value()
        ).isEqualTo(0.0);
    }
}
