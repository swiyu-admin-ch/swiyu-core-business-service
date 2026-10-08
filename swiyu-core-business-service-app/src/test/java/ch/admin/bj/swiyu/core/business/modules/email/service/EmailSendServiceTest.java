package ch.admin.bj.swiyu.core.business.modules.email.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.modules.email.domain.Email;
import ch.admin.bj.swiyu.core.business.modules.email.domain.HtmlToPlainTextConverter;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
 *
 * <p>Since EID-6921 it is also the place where the MIME structure is verified. A multipart message
 * that is nested wrongly does not fail to send: it arrives, and some clients show the markup as text
 * while others show nothing. Only looking at what the server received catches that.
 */
class EmailSendServiceTest {

    private static final String SUBJECT =
        "[TEST] Antrag eingereicht/ Application submitted/ Demande déposée/ Richiesta presentata";
    private static final String PLAIN_BODY = "Guten Tag\n\nFreundliche Grüsse\n\nBonjour\n\nBuongiorno\n\nHello";
    private static final String HTML_BODY = """
        <!DOCTYPE html>
        <html lang="de">
            <head><meta charset="utf-8" /><title>Betreff</title></head>
            <body>
                <section lang="de">
                    <p>Guten Tag</p>
                    <p>Freundliche Grüsse</p>
                    <p><a href="https://www.swiyu.ch"><img src="cid:swiyu-logo-de" alt="swiyu" /></a></p>
                </section>
            </body>
        </html>
        """;

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    private EmailSendService serviceSendingTo(int port) {
        var sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(port);
        return new EmailSendService(sender, new HtmlToPlainTextConverter());
    }

    @Test
    void deliversTheEmailToTheSmtpServer() throws Exception {
        send(email(HTML_BODY));

        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        var received = GREEN_MAIL.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo(SUBJECT);
    }

    @Test
    void preservesUmlautsAndAccentsInSubjectAndBody() throws Exception {
        var received = sendAndReceive(email(HTML_BODY));

        assertThat(received.getSubject()).contains("déposée");
        assertThat(partsOf(received).stream().map(EmailSendServiceTest::textOf)).anyMatch(part ->
            part.contains("Grüsse")
        );
    }

    @Test
    void takesSenderReplyToAndRecipientFromThePayload() throws Exception {
        var received = sendAndReceive(email(HTML_BODY));

        assertThat(received.getFrom()[0]).hasToString("registries@swiyu.admin.ch");
        assertThat(received.getReplyTo()[0]).hasToString("reply@swiyu.admin.ch");
        assertThat(received.getAllRecipients()[0]).hasToString("contact.person@partner.example.com");
    }

    @Test
    void sendsAnHtmlBodyAsMultipartWithBothAlternatives() {
        var received = sendAndReceive(email(HTML_BODY));

        var contentTypes = contentTypesOf(received);
        assertThat(contentTypes)
            .anyMatch(type -> type.startsWith("multipart/related"))
            .anyMatch(type -> type.startsWith("multipart/alternative"))
            .anyMatch(type -> type.startsWith("text/plain"))
            .anyMatch(type -> type.startsWith("text/html"));
    }

    @Test
    void derivesThePlainTextAlternativeFromTheHtml() {
        var received = sendAndReceive(email(HTML_BODY));

        var plain = partOfType(received, "text/plain");
        // The logo is a link around an image and contributes nothing a text reader could use.
        assertThat(plain).isEqualTo("Guten Tag\n\nFreundliche Grüsse").doesNotContain("cid:", "swiyu.ch");
    }

    @Test
    void attachesOneInlineImagePerContentIdTheBodyReferences() throws Exception {
        var received = sendAndReceive(email(HTML_BODY));

        var images = imageParts(received);
        assertThat(images).hasSize(1);
        assertThat(images.getFirst().getHeader("Content-ID")[0]).contains("swiyu-logo-de");
    }

    @Test
    void attachesNoImageForAContentIdThatNamesNoBundledFile() {
        var received = sendAndReceive(email(HTML_BODY.replace("cid:swiyu-logo-de", "cid:not-a-logo")));

        // An email without its logo beats no email at all, so an unknown reference is logged and
        // skipped rather than thrown.
        assertThat(imageParts(received)).isEmpty();
        assertThat(partOfType(received, "text/html")).contains("cid:not-a-logo");
    }

    @Test
    void sendsANonHtmlBodyAsSinglePartPlainTextTheWayItAlwaysDid() throws Exception {
        // The payload field is still called plainTextMessage. The sending side decides from the
        // content, so a body that is not HTML - an older message on the topic, or a future publisher -
        // goes out unchanged rather than as markup the recipient has to read by hand.
        var received = sendAndReceive(email(PLAIN_BODY));

        assertThat(received.getContentType()).startsWith("text/plain");
        assertThat(textOf(received)).isEqualTo(PLAIN_BODY);
    }

    private void send(Email email) {
        serviceSendingTo(GREEN_MAIL.getSmtp().getPort()).send(email);
    }

    private MimeMessage sendAndReceive(Email email) {
        send(email);
        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        return GREEN_MAIL.getReceivedMessages()[0];
    }

    private static List<String> contentTypesOf(MimeMessage message) {
        var types = new ArrayList<String>();
        collect(message, types, new ArrayList<>());
        return types;
    }

    private static List<Part> partsOf(MimeMessage message) {
        var parts = new ArrayList<Part>();
        collect(message, new ArrayList<>(), parts);
        return parts;
    }

    /**
     * Walks the whole MIME tree. The structure Spring builds for an inline image is nested three deep
     * - mixed, related, alternative - and asserting on the flattened tree keeps the tests readable
     * without tying them to that exact nesting.
     */
    private static void collect(Part part, List<String> types, List<Part> parts) {
        try {
            types.add(part.getContentType());
            parts.add(part);
            if (part.getContent() instanceof MimeMultipart multipart) {
                for (var i = 0; i < multipart.getCount(); i++) {
                    collect(multipart.getBodyPart(i), types, parts);
                }
            }
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException("Could not walk the MIME structure", e);
        }
    }

    private static List<Part> imageParts(MimeMessage message) {
        return partsOf(message)
            .stream()
            .filter(part -> contentTypeOf(part).startsWith("image/"))
            .toList();
    }

    private static String partOfType(MimeMessage message, String contentType) {
        return partsOf(message)
            .stream()
            .filter(part -> contentTypeOf(part).startsWith(contentType))
            .map(EmailSendServiceTest::textOf)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No %s part in the message".formatted(contentType)));
    }

    private static String contentTypeOf(Part part) {
        try {
            return part.getContentType();
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not read the content type", e);
        }
    }

    /**
     * SMTP rewrites every line ending to CRLF on the wire. That is the transport doing its job, not
     * anything the sending service decided, so it is normalised away here rather than written into
     * every expected value.
     */
    private static String textOf(Part part) {
        try {
            var content = part.getContent();
            // A part whose content is not a String is an image or a nested multipart. That is not a
            // failure, it simply has no text to contribute.
            return content instanceof String text ? text.replace("\r\n", "\n").strip() : "";
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException("Could not read the content of a MIME part", e);
        }
    }

    private static Email email(String body) {
        return new Email(
            UUID.randomUUID(),
            "SUBMISSION_ACCEPTED",
            List.of("contact.person@partner.example.com"),
            "registries@swiyu.admin.ch",
            "reply@swiyu.admin.ch",
            SUBJECT,
            Instant.now(),
            body
        );
    }
}
