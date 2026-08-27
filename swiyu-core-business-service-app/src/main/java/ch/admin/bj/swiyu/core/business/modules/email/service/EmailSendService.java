package ch.admin.bj.swiyu.core.business.modules.email.service;

import ch.admin.bj.swiyu.core.business.modules.email.domain.Email;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Hands a composed email to the SMTP gateway.
 *
 * <p>Sender, reply-to and recipients come from the {@link Email}, not from the local configuration:
 * the publishing side already decided them, and a stage that composed an email must not have it sent
 * under a different sender.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendService {

    private final JavaMailSender mailSender;

    public void send(Email email) {
        log.info("Sending email {} to {} recipient(s)", email.emailType(), email.to().size());
        mailSender.send(toMimeMessage(email));
    }

    /**
     * {@link MimeMessage} rather than {@code SimpleMailMessage}: only this way can the encoding be set
     * explicitly.
     */
    private MimeMessage toMimeMessage(Email email) {
        var message = mailSender.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(email.from());
            helper.setReplyTo(email.replyTo());
            helper.setTo(email.to().toArray(String[]::new));
            helper.setSubject(email.subject());
            helper.setText(email.plainTextMessage(), false); // false = plain text, no HTML
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Failed to compose the MIME message for the email", e);
        }
        return message;
    }
}
