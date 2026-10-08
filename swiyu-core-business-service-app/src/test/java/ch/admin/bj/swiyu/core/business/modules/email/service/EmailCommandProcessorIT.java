package ch.admin.bj.swiyu.core.business.modules.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.messaging.avro.AvroMessageIdentity;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.modules.email.domain.Email;
import ch.admin.bj.swiyu.core.business.modules.email.domain.EmailType;
import ch.admin.bj.swiyu.core.business.modules.email.domain.SentNotificationRepository;
import ch.admin.bj.swiyu.core.business.modules.email.domain.SentNotificationType;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommand;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommandPayload;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.ObjectMapper;

/**
 * The processor against a real database and a real SMTP server.
 *
 * <p>Not covered here: that a redelivered command is skipped. That is jEAP's
 * {@code @IdempotentMessageHandler} and is tested by jEAP itself.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "Test")
class EmailCommandProcessorIT {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    /**
     * What the publisher puts on the topic since EID-6921: the rendered HTML document, in the field
     * still called {@code plainTextMessage}.
     */
    private static final String HTML_BODY = """
        <!DOCTYPE html>
        <html lang="de">
            <head><meta charset="utf-8" /><title>Betreff</title></head>
            <body>
                <section lang="de"><p>Guten Tag</p></section>
                <hr />
                <section lang="fr"><p>Bonjour</p></section>
                <hr />
                <section lang="it"><p>Buongiorno</p></section>
                <hr />
                <section lang="en"><p>Hello</p></section>
            </body>
        </html>
        """;

    /**
     * A body that is not HTML, to cover the path the sending side falls back to.
     */
    private static final String PLAIN_BODY = "Guten Tag\n\nBonjour\n\nBuongiorno\n\nHello";

    /**
     * Spy, not mock: the other tests need the real send. Only the rollback test replaces the behaviour.
     */
    @MockitoSpyBean
    EmailSendService emailSendService;

    @Autowired
    EmailCommandProcessor processor;

    @Autowired
    SentNotificationRepository sentNotificationRepository;

    @MockitoBean // mocked so we don't need to bootstrap kafka (reduce pipeline time)
    private AuditPublisher auditPublisher;

    @BeforeEach
    void setUp() {
        sentNotificationRepository.deleteAll();
        reset(auditPublisher);
    }

    @Test
    void sendsTheEmailAndRecordsIt() {
        // GIVEN
        var command = command(UUID.randomUUID().toString());

        // WHEN
        processor.process(command);

        // THEN
        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        assertThat(sentNotificationRepository.findAll())
            .singleElement()
            .satisfies(notification -> {
                assertThat(notification.getType()).isEqualTo(SentNotificationType.EMAIL);
                // Email type, recipients, subject and body all live inside the stored Email record
                var email = notification.getEmail();
                assertThat(email.get("emailType").asString()).isEqualTo("SUBMISSION_ACCEPTED");
                assertThat(email.get("to").get(0).asString()).isEqualTo("contact.person@partner.example.com");
                assertThat(email.get("subject").asString()).contains("Antrag eingereicht");
            });
        verifyAuditCommandWasSent((command.getPayload()).getPartnerId());
    }

    @Test
    void deliversAnHtmlBodyAsAMultipartMessageWithBothAlternatives() throws Exception {
        processor.process(command(UUID.randomUUID().toString()));

        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        var received = GREEN_MAIL.getReceivedMessages()[0];

        assertThat(received.getContentType()).startsWith("multipart/");
        var types = contentTypesOf(received);
        assertThat(types)
            .anyMatch(type -> type.startsWith("text/plain"))
            .anyMatch(type -> type.startsWith("text/html"));
    }

    @Test
    void deliversABodyThatIsNotHtmlAsSinglePartPlainText() throws Exception {
        // The field is still called plainTextMessage. Whatever else ends up on the topic - an older
        // message, a future publisher - must still reach the partner as something readable.
        processor.process(command(UUID.randomUUID().toString(), PLAIN_BODY));

        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        assertThat(GREEN_MAIL.getReceivedMessages()[0].getContentType()).startsWith("text/plain");
    }

    private static List<String> contentTypesOf(Part part) throws MessagingException, IOException {
        var types = new ArrayList<String>();
        types.add(part.getContentType());
        if (part.getContent() instanceof MimeMultipart multipart) {
            for (var i = 0; i < multipart.getCount(); i++) {
                types.addAll(contentTypesOf(multipart.getBodyPart(i)));
            }
        }
        return types;
    }

    /**
     * The most important test of this story. If the record survived a failed send, it would block every
     * retry and the email would never go out at all.
     *
     * <p>The failure is injected into the send service rather than by stopping GreenMail: its
     * {@code stop()} still calls {@code Thread.stop()}, which was removed in Java 25. How the send
     * fails is irrelevant here - what matters is that neither the SentNotification nor the idempotence
     * record survives it. Were the latter to survive, the redelivered command would be treated as
     * already processed and the email would never go out.
     */
    @Test
    void rollsBackTheRecordWhenSendingFails() {
        doThrow(new MailSendException("smtp gateway unreachable")).when(emailSendService).send(any());
        var command = command(UUID.randomUUID().toString());

        assertThatThrownBy(() -> processor.process(command)).isInstanceOf(MailSendException.class);

        assertThat(sentNotificationRepository.findAll()).isEmpty();
    }

    /**
     * The idempotence aspect runs on every call and reads the idempotence id and the message type
     * name, so both have to be stubbed even though no test here asserts on idempotence. A stub avoids
     * depending on the full Avro message envelope.
     */
    private static TiSendEmailCommand command(String idempotenceId) {
        return command(idempotenceId, HTML_BODY);
    }

    private static TiSendEmailCommand command(String idempotenceId, String body) {
        var identity = mock(AvroMessageIdentity.class);
        when(identity.getIdempotenceId()).thenReturn(idempotenceId);

        var type = mock(AvroMessageType.class);
        when(type.getName()).thenReturn("TiSendEmailCommand");

        var payload = new TiSendEmailCommandPayload(
            UUID.randomUUID(),
            "SUBMISSION_ACCEPTED",
            List.of("contact.person@partner.example.com"),
            "registries@swiyu.admin.ch",
            "registries@swiyu.admin.ch",
            "[TEST] Antrag eingereicht/ Application submitted/ Demande déposée/ Richiesta presentata",
            Instant.now(),
            body
        );

        var command = mock(TiSendEmailCommand.class);
        when(command.getIdentity()).thenReturn(identity);
        when(command.getType()).thenReturn(type);
        when(command.getPayload()).thenReturn(payload);
        return command;
    }

    private void verifyAuditCommandWasSent(UUID partnerId) {
        var partnerIdCaptor = ArgumentCaptor.forClass(UUID.class);
        var emailJsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher, times(1)).emailSent(partnerIdCaptor.capture(), any(), emailJsonCaptor.capture());
        var emailJson = emailJsonCaptor.getValue();
        var email = new ObjectMapper().readValue(emailJson, Email.class);
        assertThat(partnerIdCaptor.getValue()).isEqualTo(partnerId);
        assertThat(email.emailType()).isEqualTo(EmailType.SUBMISSION_ACCEPTED.toString());
    }
}
