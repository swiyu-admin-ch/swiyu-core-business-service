package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.IdentifierStatusDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports the total number of business partners, status lists and identifier entries as Prometheus
 * metrics, broken down by business partner type and identifier status. The values are (re-)evaluated
 * by a scheduler every {@code app.jobs.statistics.export-interval} (unit defined by the value).
 * Active only when {@code app.jobs.statistics.enabled=true}.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.jobs.statistics", name = "enabled", havingValue = "true")
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class StatisticsMetricsExporterJob {

    private final BusinessPartnerService businessPartnerService;
    private final StatusListEntryService statusListEntryService;
    private final IdentifierEntryService identifierEntryService;
    private final MeterRegistry meterRegistry;

    private final AtomicLong businessPartnerCount = new AtomicLong();
    private final AtomicLong statusListCount = new AtomicLong();
    private final AtomicLong identifierCount = new AtomicLong();

    private final Map<BusinessPartnerTypeDto, AtomicLong> businessPartnerCountByType = new EnumMap<>(
        BusinessPartnerTypeDto.class
    );
    private final Map<IdentifierStatusDto, AtomicLong> identifierCountByStatus = new EnumMap<>(
        IdentifierStatusDto.class
    );

    @PostConstruct
    public void setup() {
        Gauge.builder("swiyu_cbs_business_partner", businessPartnerCount, AtomicLong::get)
            .description("Total number of business partners")
            .register(meterRegistry);
        Gauge.builder("swiyu_cbs_status_list", statusListCount, AtomicLong::get)
            .description("Total number of status lists")
            .register(meterRegistry);
        Gauge.builder("swiyu_cbs_identifier", identifierCount, AtomicLong::get)
            .description("Total number of identifier entries")
            .register(meterRegistry);

        for (BusinessPartnerTypeDto type : BusinessPartnerTypeDto.values()) {
            var count = new AtomicLong();
            businessPartnerCountByType.put(type, count);
            Gauge.builder("swiyu_cbs_business_partner_by_type", count, AtomicLong::get)
                .description("Number of business partners")
                .tag("type", type.name())
                .register(meterRegistry);
        }

        for (IdentifierStatusDto status : IdentifierStatusDto.values()) {
            var count = new AtomicLong();
            identifierCountByStatus.put(status, count);
            Gauge.builder("swiyu_cbs_identifier_by_status", count, AtomicLong::get)
                .description("Number of identifier entries")
                .tag("status", status.name())
                .register(meterRegistry);
        }
        // initial update
        refreshStatistics();
    }

    @Scheduled(cron = "${app.jobs.statistics.export-interval}")
    @Transactional(readOnly = true)
    public void refreshStatisticsJob() {
        refreshStatistics();
    }

    public void refreshStatistics() {
        businessPartnerCount.set(businessPartnerService.count());
        statusListCount.set(statusListEntryService.count());
        identifierCount.set(identifierEntryService.count());

        for (var entry : businessPartnerCountByType.entrySet()) {
            entry.getValue().set(businessPartnerService.countByType(entry.getKey()));
        }

        for (var entry : identifierCountByStatus.entrySet()) {
            entry.getValue().set(identifierEntryService.countByStatus(entry.getKey()));
        }
    }
}
