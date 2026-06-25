package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustOnboardingSubmissionCleanupJob {

    private final TrustOnboardingService trustOnboardingService;

    @Timed
    @Scheduled(initialDelay = 10_000, fixedRateString = "${app.jobs.trust-onboarding-submission-cleanup-interval}")
    @SchedulerLock(name = "TrustOnboardingSubmissionCleanupJob")
    public void triggerTrustOnboardingSubmissionCheckForUnsubmittedTimeout() {
        log.debug("Triggering job to check for unsubmitted TrustOnboardingSubmissions that have timed out.");
        trustOnboardingService.trustOnboardingSubmissionCheckForUnsubmittedTimeout();
    }
}
