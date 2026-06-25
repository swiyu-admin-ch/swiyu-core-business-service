package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmissionStatus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@WithJeapAuthenticationToken(username = "Test")
@WithAllTestContainerInitializers
class TrustAdditionalDidsSubmissionCleanupJobIT {

    @Autowired
    private TestRepositories testRepositories;

    @Autowired
    private TrustAdditionalDidsSubmissionRepository repository;

    @Autowired
    private TrustAdditionalDidsSubmissionCleanupJob job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        testRepositories.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(testRepositories.businessPartner);
    }

    @Test
    void testTrustAdditionalDidsSubmissionTimesOut() {
        var sharedNonce = UUID.randomUUID().toString();
        var submissionA = repository.save(
            new TrustAdditionalDidsSubmission(
                BusinessEntityTestData.ENTITY_A,
                new ProofOfPossession("did:tdw:example.com:permA", sharedNonce),
                List.of(new ProofOfPossession("did:tdw:example.com:addA", sharedNonce))
            )
        );
        var submissionB = repository.save(
            new TrustAdditionalDidsSubmission(
                BusinessEntityTestData.ENTITY_B,
                new ProofOfPossession("did:tdw:example.com:permB", sharedNonce),
                List.of(new ProofOfPossession("did:tdw:example.com:addB", sharedNonce))
            )
        );

        // Backdate submissionA's createdAt so it exceeds the max-age-in-unsubmitted (1m in test config)
        var oldTimestamp = Instant.now().minus(Duration.ofDays(1000));
        jdbcTemplate.update(
            "UPDATE trust_additional_dids_submission SET created_at = ? WHERE id = ?",
            java.sql.Timestamp.from(oldTimestamp),
            submissionA.getId()
        );

        // Validate both are initially UNSUBMITTED
        assertThat(repository.findById(submissionA.getId()).orElseThrow().getStatus()).isEqualTo(
            TrustAdditionalDidsSubmissionStatus.UNSUBMITTED
        );
        assertThat(repository.findById(submissionB.getId()).orElseThrow().getStatus()).isEqualTo(
            TrustAdditionalDidsSubmissionStatus.UNSUBMITTED
        );

        // Run the cleanup scheduler
        job.triggerTrustAdditionalDidsSubmissionCheckForUnsubmittedTimeout();

        // Validate submissionA was expired, submissionB was not
        assertThat(repository.findById(submissionA.getId()).orElseThrow().getStatus()).isEqualTo(
            TrustAdditionalDidsSubmissionStatus.UNSUBMITTED_TIMEOUT
        );
        assertThat(repository.findById(submissionB.getId()).orElseThrow().getStatus()).isEqualTo(
            TrustAdditionalDidsSubmissionStatus.UNSUBMITTED
        );
    }
}
