package ch.admin.bj.swiyu.core.business.modules.email.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeGov;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerWithoutContactEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.modules.email.domain.EmailType;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommand;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against a real business partner: the publisher resolves the recipient from the partner id
 * itself, so a stubbed lookup would hide exactly the behaviour under test here.
 */
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
class EmailCommandPublisherIT {

    @Autowired
    EmailCommandPublisher emailCommandPublisher;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @Autowired
    TestRepositories repos;

    @MockitoBean // registers a callback so we can verify the sent message
    JeapKafkaMessageCallback kafkaMsgCallback;

    @BeforeEach
    void setUp() {
        var messages = deferredMessageRepository.findAll();
        deferredMessageRepository.deleteAllById(
            messages.stream().map(DeferredMessage::getId).collect(Collectors.toSet())
        );
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void putsTheComposedEmailIntoTheTransactionalOutbox() {
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        var partnerId = partner.getId();

        emailCommandPublisher.submissionAccepted(partnerId);

        assertThat(deferredMessageRepository.findAll()).hasSize(1);

        var messageCaptor = ArgumentCaptor.forClass(TiSendEmailCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();

        assertThat(msg.getPublisher().getSystem()).isEqualTo("ti");
        assertThat(msg.getPublisher().getService()).isEqualTo("swiyu-core-business-service");

        var payload = msg.getPayload();
        assertThat(payload.getPartnerId()).isEqualTo(partnerId);
        assertThat(payload.getEmailType()).isEqualTo(EmailType.SUBMISSION_ACCEPTED.name());
        // The recipient is resolved from the partner, not passed in by the caller
        assertThat(payload.getTo()).containsExactly(partner.getContact().getEmail());
        assertThat(payload.getFrom()).isEqualTo("registries@swiyu.admin.ch");
        assertThat(payload.getReplyTo()).isEqualTo("registries@swiyu.admin.ch");
        assertThat(payload.getSentAt()).isNotNull();
        assertThat(payload.getSubject()).isEqualTo(
            "[TEST] Antrag eingereicht/ Application submitted/ Demande déposée/ Richiesta presentata"
        );
        assertThat(payload.getPlainTextMessage())
            .containsSubsequence("Guten Tag", "Bonjour", "Buongiorno", "Hello")
            .contains("https://portal.trust-infra.swiyu.admin.ch/ui/business-partners/" + partnerId);
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void publishesNothingForAPartnerWithoutContactEmail() {
        var partner = repos.businessPartner.save(businessPartnerWithoutContactEmail(UUID.randomUUID()));

        emailCommandPublisher.submissionAccepted(partner.getId());

        assertThat(deferredMessageRepository.findAll()).isEmpty();
    }
}
