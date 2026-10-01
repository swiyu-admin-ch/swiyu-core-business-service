package ch.admin.bj.swiyu.core.business.modules.dataimport.service;

import static ch.admin.bj.swiyu.core.business.common.security.SystemUserAuthenticationSupport.setSystemSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles({ "test", "test-data-injection" })
@SpringBootTest
@WithAllTestContainerInitializers
class DemoDataImportServiceIT {

    @Autowired
    DemoDataImportService demoDataImportService;

    @Autowired
    TestRepositories repos;

    @MockitoBean
    ScanApi scanApi;

    @MockitoBean
    ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher auditPublisher;

    @MockitoBean
    ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader didPublicKeyLoader;

    @MockitoBean
    DemoDataAsyncExecutor demoDataAsyncExecutor;

    @BeforeEach
    void setUp() {
        repos.truncateTables();

        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .result("OK")
                    .requestID(UUID.randomUUID())
                    .description("description")
                    .clamavVersion("clamav-v1")
                    .clamavDatabaseVersion("clamav-db-v1")
            )
        );
    }

    @Test
    void generatedSubmissionsPersistTheirType() {
        setSystemSecurityContext();
        demoDataImportService.generateBusinessPartners();
        demoDataImportService.generateTrustOnboardingSubmissions();

        var renewal = repos.trustOnboardingSubmission.findById(UUID.fromString("4b1a77c2-9d3e-4f2a-8c5d-1e0b6f7a8c9d"));
        assertThat(renewal).isPresent();
        assertThat(renewal.orElseThrow().getType()).isEqualTo(TrustOnboardingSubmissionType.RENEWAL);

        var profileChange = repos.trustOnboardingSubmission.findById(
            UUID.fromString("3299cd25-8bab-47b7-9d46-f740be76e57e")
        );
        assertThat(profileChange).isPresent();
        assertThat(profileChange.orElseThrow().getType()).isEqualTo(
            TrustOnboardingSubmissionType.PROFILE_CHANGE_MANDATORY
        );
    }
}
