package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.admin.bj.swiyu.core.business.modules.jobs.config.PaymentReconciliationJobProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentReconciliationJobTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private PaymentReconciliationJob job;

    @BeforeEach
    void setUp() {
        var properties = new PaymentReconciliationJobProperties(300_000, 5, 24, 50);
        job = new PaymentReconciliationJob(paymentService, properties);
    }

    @Test
    void reconcilesWithTheConfiguredWindowAndBatchSize() {
        job.reconcilePendingPayments();

        verify(paymentService).reconcilePendingPayments(Duration.ofMinutes(5), Duration.ofHours(24), 50);
    }
}
