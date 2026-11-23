package ch.admin.bj.swiyu.core.business.modules.jobs.config;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the payment reconciliation job.
 *
 * @param intervalMs         how often the job runs
 * @param gracePeriodMinutes how long to wait after the Saferpay session expired, so the webhook can come first
 * @param lookbackHours      older payments are ignored
 * @param batchSize          how many payments are loaded at once, the job handles all of them
 */
@Validated
@ConfigurationProperties(prefix = "app.jobs.payment-reconciliation")
public record PaymentReconciliationJobProperties(
    @Positive long intervalMs,
    @Positive int gracePeriodMinutes,
    @Positive int lookbackHours,
    @Positive int batchSize
) {
    public Duration gracePeriod() {
        return Duration.ofMinutes(gracePeriodMinutes);
    }

    public Duration lookback() {
        return Duration.ofHours(lookbackHours);
    }
}
