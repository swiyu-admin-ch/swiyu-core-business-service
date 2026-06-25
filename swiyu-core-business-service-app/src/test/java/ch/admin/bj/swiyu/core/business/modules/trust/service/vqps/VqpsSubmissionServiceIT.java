package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import static ch.admin.bj.swiyu.core.business.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;
import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.vqpsJwt;
import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.vqpsSubmission;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.modules.trust.api.DcqlQueryValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsPublicationFailureReasonDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.nimbusds.jose.JOSEException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@DataJpaTest
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "test")
@Import({ DataJpaTestConfiguration.class, VqpsSubmissionService.class })
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VqpsSubmissionServiceIT {

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @MockitoBean
    DcqlQueryValidator dcqlQueryValidator;

    @Autowired
    TestRepositories repos;

    @Autowired
    VqpsSubmissionService vqpsSubmissionService;

    @BeforeEach
    void setUp() {
        repos.vqpsSubmission.deleteAllInBatch();
    }

    @Test
    void getVqpsSubmissionInternal() {
        // GIVEN
        var partnerId = UUID.fromString("deadbeef-0000-0000-0000-000000000000");
        var submission = repos.vqpsSubmission.save(vqpsSubmission(partnerId));
        repos.commit();
        // WHEN
        var result = vqpsSubmissionService.getVqpsSubmissionInternal(submission.getId());
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(VqpsSubmissionStatusDto.ACCEPTED);
        assertThat(result.purposeName()).containsEntry(DEFAULT_VALUE_KEY, "purpose name");
        assertThat(result.purposeName()).containsEntry("de-CH", "purpose name de");
        assertThat(result.purposeDescription()).containsEntry(DEFAULT_VALUE_KEY, "purpose description");
        assertThat(result.purposeDescription()).containsEntry("de-CH", "purpose description de");
        assertThat(result.query()).isNotNull();
    }

    @Test
    void markAsPublicationSucceeded()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        // GIVEN
        var partnerId = UUID.fromString("deadbeef-0000-0000-0000-000000000000");
        var submissionId = repos.vqpsSubmission.save(vqpsSubmission(partnerId)).getId();
        repos.commit();
        // WHEN
        var jti = UUID.randomUUID();
        var expiresAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var jwt = vqpsJwt(jti, expiresAt);
        vqpsSubmissionService.markAsPublicationSucceeded(submissionId, jwt);
        var result = vqpsSubmissionService.getVqpsSubmissionB2B(submissionId);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(VqpsSubmissionStatusDto.PUBLICATION_SUCCEEDED);
        assertThat(result.publicationResult()).isNotNull();
        assertThat(result.publicationResult().jti()).isEqualTo(jti.toString());
        assertThat(result.publicationResult().jwt()).isEqualTo(jwt);
        assertThat(result.publicationResult().expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void markAsPublicationFailed() {
        // GIVEN
        var partnerId = UUID.fromString("deadbeef-0000-0000-0000-000000000000");
        var submissionId = repos.vqpsSubmission.save(vqpsSubmission(partnerId)).getId();
        repos.commit();
        // WHEN
        vqpsSubmissionService.markAsPublicationFailed(submissionId, VqpsPublicationFailureReasonDto.UNKNOWN);
        var result = vqpsSubmissionService.getVqpsSubmissionB2B(submissionId);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(VqpsSubmissionStatusDto.PUBLICATION_FAILED);
        assertThat(result.publicationResult()).isNull();
        assertThat(result.publicationFailureReason()).isEqualTo(VqpsPublicationFailureReasonDto.UNKNOWN);
    }
}
