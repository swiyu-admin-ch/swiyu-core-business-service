package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import ch.admin.bj.swiyu.core.business.modules.email.service.EmailNotificationService;
import ch.admin.bj.swiyu.core.business.modules.jobs.config.EmailNotificationJobProperties;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Publishes the scheduled reminder emails once a night.
 *
 * <p>Two deviations from the three cleanup jobs next to it, both deliberate.
 *
 * <p>{@code cron} instead of {@code fixedRateString}: the others run on an interval because it does not
 * matter when they run. This one sends mail, so it has to hit the night rather than a random minute of
 * the working day.
 *
 * <p>{@code lockAtMostFor = "30m"} instead of the {@code defaultLockAtMostFor = "10m"} set on
 * {@code Application}: a run across the whole partner base can exceed ten minutes, and ShedLock would
 * then release the lock while the first pod is still working. Only the idempotence id would keep the
 * second pod from sending everything twice - which is a guard, not a reason to provoke it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationJob {

    private final EmailNotificationService emailNotificationService;
    private final EmailNotificationJobProperties properties;

    @Timed
    @Scheduled(cron = "${app.jobs.email-notification.cron}")
    @SchedulerLock(name = "EmailNotificationJob", lockAtMostFor = "30m")
    public void triggerScheduledEmailNotifications() {
        log.debug("Triggering job to publish due email notifications.");
        emailNotificationService.publishDueNotifications(properties.window(), properties.pageSize());
    }
}
