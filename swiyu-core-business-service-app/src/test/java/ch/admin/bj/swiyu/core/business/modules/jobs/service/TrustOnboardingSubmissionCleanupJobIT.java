package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionDomainService;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@WithJeapAuthenticationToken(username = "Test")
@WithAllTestContainerInitializers
class TrustOnboardingSubmissionCleanupJobIT {

    @Autowired
    private TestRepositories testRepositories;

    @Autowired
    private TrustOnboardingSubmissionDomainService trustOnboardingSubmissionDomainService;

    @Autowired
    private TrustOnboardingSubmissionCleanupJob job;

    @BeforeEach
    void setUp() {
        testRepositories.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(testRepositories.businessPartner);
    }

    @Test
    void testTrustOnboardingSubmissionTimesOut() {
        var tosA = trustOnboardingSubmission(
            UUID.randomUUID(),
            BusinessEntityTestData.ENTITY_A,
            Instant.now().minus(Duration.ofDays(1000))
        );
        var tosB = trustOnboardingSubmission(UUID.randomUUID(), BusinessEntityTestData.ENTITY_B);
        testRepositories.trustOnboardingSubmission.saveAll(List.of(tosA, tosB));
        // validate everything is unsubmitted
        var tosACheck = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(tosA.getId());
        var tosBCheck = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(tosB.getId());
        assertThat(tosACheck.getStatus()).isEqualTo(TrustOnboardingSubmissionStatus.UNSUBMITTED);
        assertThat(tosBCheck.getStatus()).isEqualTo(TrustOnboardingSubmissionStatus.UNSUBMITTED);

        // Run the scheduler
        job.triggerTrustOnboardingSubmissionCheckForUnsubmittedTimeout();

        // validate tosA was expired
        tosACheck = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(tosA.getId());
        tosBCheck = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(tosB.getId());
        assertThat(tosACheck.getStatus()).isEqualTo(TrustOnboardingSubmissionStatus.UNSUBMITTED_TIMEOUT);
        assertThat(tosBCheck.getStatus()).isEqualTo(TrustOnboardingSubmissionStatus.UNSUBMITTED);
    }
}
