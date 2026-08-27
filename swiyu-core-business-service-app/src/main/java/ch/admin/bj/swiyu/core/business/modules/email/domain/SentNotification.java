package ch.admin.bj.swiyu.core.business.modules.email.domain;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tools.jackson.databind.JsonNode;

/**
 * Record of a notification email that was handed to the SMTP gateway.
 *
 * <p>Answers which email a partner received and when. The scheduled reminders (EID-6628) decide from
 * it whether a reminder already went out. Preventing a duplicate send is not this entity's job -
 * that is {@code @IdempotentMessageHandler} on the processor.
 */
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SentNotification {

    @Embedded
    @Valid
    @NotNull
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    private UUID id;

    /**
     * Unique id of one specific notification. A generated UUID for the event driven emails; the
     * scheduled reminders (EID-6628) will use composed business keys, hence text and not uuid.
     */
    @NotBlank
    private String idempotenceId;

    /**
     * Channel this notification went through. Only {@code EMAIL} today.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private SentNotificationType type;

    private UUID partnerId;

    /**
     * The email as it was sent. Everything else about the notification - type, recipients, subject,
     * body - is in here.
     */
    @NotNull
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode email;

    /**
     * When the gateway accepted the message. Deliberately not {@code Email.composedAt}, which is the
     * moment the email was composed - after an outage the two are hours apart.
     */
    @NotNull
    private Instant sentAt;

    public SentNotification(
        String idempotenceId,
        SentNotificationType type,
        UUID partnerId,
        JsonNode email,
        Instant sentAt
    ) {
        this.id = UUID.randomUUID();
        this.idempotenceId = idempotenceId;
        this.type = type;
        this.partnerId = partnerId;
        this.email = email;
        this.sentAt = sentAt;
    }
}
