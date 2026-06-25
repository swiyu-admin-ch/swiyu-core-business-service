package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustOnboardingDocumentsCleanupJob {

    private final PartnerDocumentService partnerDocumentService;

    @Timed
    @Scheduled(initialDelay = 10_000, fixedRateString = "${app.jobs.trust-onboarding-document-cleanup-interval}")
    @SchedulerLock(name = "TrustOnboardingDocumentsCleanupJob")
    public void triggerCleanupTrustOnboardingSubmissionDocuments() {
        log.debug(
            "Triggering job to clean up documents of unsubmitted TrustOnboardingSubmissions that have timed out."
        );
        partnerDocumentService.cleanupTrustOnboardingSubmissionDocuments();
    }
}
