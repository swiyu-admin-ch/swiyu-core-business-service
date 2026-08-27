package ch.admin.bj.swiyu.core.business.modules.email.domain;

import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommandPayload;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A fully composed email, ready to be sent.
 *
 * <p>Deliberately a record and not the Avro payload: this is what gets stored as JSON on
 * {@link SentNotification}, and a record serialises to a stable, predictable shape. A generated
 * {@code SpecificRecord} carries schema accessors that would end up in the JSON, and the stored shape
 * would change with every regeneration of the message type.
 *
 * @param partnerId        the business partner this email is about
 * @param emailType        name of the {@link EmailType} the email was composed from
 * @param to               recipients, today always the contact person of the partner
 * @param from             sender address of the stage
 * @param replyTo          reply-to address of the stage
 * @param subject          all four languages, separated by "/", with the stage prefix
 * @param composedAt       when the email was composed - not when it was sent
 * @param plainTextMessage the body, four sections DE / FR / IT / EN
 */
public record Email(
    UUID partnerId,
    String emailType,
    List<String> to,
    String from,
    String replyTo,
    String subject,
    Instant composedAt,
    String plainTextMessage
) {
    public static Email from(TiSendEmailCommandPayload payload) {
        return new Email(
            payload.getPartnerId(),
            payload.getEmailType(),
            List.copyOf(payload.getTo()),
            payload.getFrom(),
            payload.getReplyTo(),
            payload.getSubject(),
            payload.getSentAt(),
            payload.getPlainTextMessage()
        );
    }
}
