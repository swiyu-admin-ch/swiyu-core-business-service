package ch.admin.bj.swiyu.core.business.modules.dataimport.service;

import static ch.admin.bj.swiyu.core.business.common.security.SystemUserAuthenticationSupport.setSystemSecurityContext;

import ch.admin.bj.swiyu.core.business.common.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("test-data-injection")
public class DemoDataAsyncExecutor {

    private final AsyncService async;
    private final DemoDataImportService demoDataImportService;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTestData() {
        async.run(() -> {
            try {
                log.warn("LOCAL TEST DATA INJECTION is happening!");
                // setting system auth for AuditMetadata
                setSystemSecurityContext();

                var partnerIds = demoDataImportService.generateBusinessPartners();
                demoDataImportService.generateIdentifierEntries(partnerIds);
                demoDataImportService.deleteDemoTrustOnboardingSubmissions();
                demoDataImportService.generateTrustOnboardingSubmissions();
                log.debug("LOCAL TEST DATA INJECTION is done!");
            } catch (Exception e) {
                log.error("LOCAL TEST DATA INJECTION failed!", e);
            }
        });
    }
}
