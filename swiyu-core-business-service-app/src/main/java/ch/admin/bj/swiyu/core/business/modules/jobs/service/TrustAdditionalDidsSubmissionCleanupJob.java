package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustAdditionalDidsService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustAdditionalDidsSubmissionCleanupJob {

    private final TrustAdditionalDidsService trustAdditionalDidsService;

    @Timed
    @Scheduled(initialDelay = 10_000, fixedRateString = "${app.jobs.trust-additional-dids-submission-cleanup-interval}")
    @SchedulerLock(name = "TrustAdditionalDidsSubmissionCleanupJob")
    public void triggerTrustAdditionalDidsSubmissionCheckForUnsubmittedTimeout() {
        log.debug("Triggering job to check for unsubmitted TrustAdditionalDidsSubmissions that have timed out.");
        trustAdditionalDidsService.trustAdditionalDidsSubmissionCheckForUnsubmittedTimeout();
    }
}
