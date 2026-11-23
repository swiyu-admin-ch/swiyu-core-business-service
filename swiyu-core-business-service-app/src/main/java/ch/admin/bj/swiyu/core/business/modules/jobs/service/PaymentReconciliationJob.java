package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import ch.admin.bj.swiyu.core.business.modules.jobs.config.PaymentReconciliationJobProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Safety net for payments whose Saferpay webhook never arrived: finalizes them like the webhook would.
 * Runs on one pod at a time (ShedLock). The first run waits one interval, so it does not fire during tests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentService paymentService;
    private final PaymentReconciliationJobProperties properties;

    @Timed
    @Scheduled(
        initialDelayString = "${app.jobs.payment-reconciliation.interval-ms}",
        fixedRateString = "${app.jobs.payment-reconciliation.interval-ms}"
    )
    @SchedulerLock(name = "PaymentReconciliationJob")
    public void reconcilePendingPayments() {
        log.debug("Triggering job to reconcile pending payments.");
        var count = paymentService.reconcilePendingPayments(
            properties.gracePeriod(),
            properties.lookback(),
            properties.batchSize()
        );
        if (count > 0) {
            log.info("Published change notifications for {} pending payments.", count);
        }
    }
}
