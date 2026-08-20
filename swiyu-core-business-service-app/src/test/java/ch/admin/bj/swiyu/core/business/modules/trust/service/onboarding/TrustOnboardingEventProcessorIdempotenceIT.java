package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_B;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSucceededEvent;
import ch.admin.bj.swiyu.messagetype.ti.TrustOnboardingSucceededPayload;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Proves that {@code @IdempotentMessageHandler} is actually wired up on the event processors: a
 * redelivered event must not trigger the state change - and therefore not the email - a second time.
 *
 * <p>Runs against the real {@link TrustOnboardingService} and a real database. Only the outbound
 * ports are mocked. The email publisher is the one that makes a duplicate delivery observable:
 * marking a submission as succeeded twice leaves the same row state behind, but sending the partner
 * two "registration succeeded" mails is exactly the damage idempotence has to prevent.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "Test")
class TrustOnboardingEventProcessorIdempotenceIT {

    @MockitoBean
    EmailCommandPublisher emailCommandPublisher;

    @MockitoBean
    AuditPublisher auditPublisher;

    @MockitoBean
    PamsClient pamsClient;

    @Autowired
    TrustOnboardingEventProcessor processor;

    @Autowired
    TestRepositories repos;

    @BeforeEach
    void setUp() {
        repos.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(repos.businessPartner);
    }

    @Test
    void processesARedeliveredEventOnlyOnce() {
        var submissionId = givenSubmittedSubmission(ENTITY_A);
        var event = succeededEvent(submissionId, UUID.randomUUID().toString());

        processor.processOnboardingRequestAcceptedEvent(event);
        processor.processOnboardingRequestAcceptedEvent(event);

        assertThat(statusOf(submissionId)).isEqualTo(TrustOnboardingSubmissionStatus.SUCCEEDED);
        verify(emailCommandPublisher, times(1)).trustRegistrationSucceeded(ENTITY_A);
    }

    @Test
    void processesTwoDistinctEvents() {
        var firstSubmissionId = givenSubmittedSubmission(ENTITY_A);
        var secondSubmissionId = givenSubmittedSubmission(ENTITY_B);

        processor.processOnboardingRequestAcceptedEvent(
            succeededEvent(firstSubmissionId, UUID.randomUUID().toString())
        );
        processor.processOnboardingRequestAcceptedEvent(
            succeededEvent(secondSubmissionId, UUID.randomUUID().toString())
        );

        assertThat(statusOf(firstSubmissionId)).isEqualTo(TrustOnboardingSubmissionStatus.SUCCEEDED);
        assertThat(statusOf(secondSubmissionId)).isEqualTo(TrustOnboardingSubmissionStatus.SUCCEEDED);
        verify(emailCommandPublisher).trustRegistrationSucceeded(ENTITY_A);
        verify(emailCommandPublisher).trustRegistrationSucceeded(ENTITY_B);
    }

    private UUID givenSubmittedSubmission(UUID partnerId) {
        var submission = trustOnboardingSubmission(
            UUID.randomUUID(),
            partnerId,
            TrustOnboardingSubmissionStatus.SUBMITTED
        );
        return repos.trustOnboardingSubmission.save(submission).getId();
    }

    private TrustOnboardingSubmissionStatus statusOf(UUID submissionId) {
        return repos.trustOnboardingSubmission.findById(submissionId).orElseThrow().getStatus();
    }

    /**
     * The idempotence aspect only reads the idempotence id and the message type name, so a stub is
     * enough here and avoids depending on the full Avro message envelope.
     */
    private static TiTrustOnboardingSucceededEvent succeededEvent(UUID submissionId, String idempotenceId) {
        var identity = mock(AvroDomainEventIdentity.class);
        when(identity.getIdempotenceId()).thenReturn(idempotenceId);

        var type = mock(AvroDomainEventType.class);
        when(type.getName()).thenReturn("TiTrustOnboardingSucceededEvent");

        var payload = new TrustOnboardingSucceededPayload(submissionId, null);

        var event = mock(TiTrustOnboardingSucceededEvent.class);
        when(event.getIdentity()).thenReturn(identity);
        when(event.getType()).thenReturn(type);
        when(event.getPayload()).thenReturn(payload);
        return event;
    }
}
