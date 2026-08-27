package ch.admin.bj.swiyu.core.business.modules.email.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.modules.email.domain.Email;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Sends against a real in-memory SMTP server and inspects what arrives.
 *
 * <p>This is the only place where the header encoding can be verified. The subjects carry "déposée"
 * and "Vérification"; they already survived Thymeleaf, Avro and Kafka in EID-6626 - SMTP, with its own
 * header encoding, is the last place where they can still break.
 */
class EmailSendServiceTest {

    private static final String SUBJECT =
        "[TEST] Antrag eingereicht/ Application submitted/ Demande déposée/ Richiesta presentata";
    private static final String BODY = "Guten Tag\n\nFreundliche Grüsse\n\nBonjour\n\nBuongiorno\n\nHello";

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    private EmailSendService serviceSendingTo(int port) {
        var sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(port);
        return new EmailSendService(sender);
    }

    @Test
    void deliversTheEmailToTheSmtpServer() throws Exception {
        serviceSendingTo(GREEN_MAIL.getSmtp().getPort()).send(email());

        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        var received = GREEN_MAIL.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo(SUBJECT);
    }

    @Test
    void preservesUmlautsAndAccentsInSubjectAndBody() throws Exception {
        serviceSendingTo(GREEN_MAIL.getSmtp().getPort()).send(email());

        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        var received = GREEN_MAIL.getReceivedMessages()[0];

        assertThat(received.getSubject()).contains("déposée");
        assertThat(bodyOf(received)).contains("Grüsse");
    }

    @Test
    void takesSenderReplyToAndRecipientFromThePayload() throws Exception {
        serviceSendingTo(GREEN_MAIL.getSmtp().getPort()).send(email());

        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        var received = GREEN_MAIL.getReceivedMessages()[0];

        assertThat(received.getFrom()[0]).hasToString("registries@swiyu.admin.ch");
        assertThat(received.getReplyTo()[0]).hasToString("reply@swiyu.admin.ch");
        assertThat(received.getAllRecipients()[0]).hasToString("contact.person@partner.example.com");
    }

    @Test
    void sendsThePlainTextBodyWithoutHtml() throws Exception {
        serviceSendingTo(GREEN_MAIL.getSmtp().getPort()).send(email());

        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        assertThat(GREEN_MAIL.getReceivedMessages()[0].getContentType()).startsWith("text/plain");
    }

    private static String bodyOf(MimeMessage message) throws Exception {
        return message.getContent().toString();
    }

    private static Email email() {
        return new Email(
            UUID.randomUUID(),
            "SUBMISSION_ACCEPTED",
            List.of("contact.person@partner.example.com"),
            "registries@swiyu.admin.ch",
            "reply@swiyu.admin.ch",
            SUBJECT,
            Instant.now(),
            BODY
        );
    }
}
