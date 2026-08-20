package ch.admin.bj.swiyu.core.business.modules.email.domain;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommand;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommandPayload;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TiSendEmailCommandBuilder extends AvroCommandBuilder<TiSendEmailCommandBuilder, TiSendEmailCommand> {

    private static final String SERVICE_NAME = "swiyu-core-business-service"; // spring application name
    private static final String SYSTEM_NAME = "ti"; // trust infrastructure

    private UUID partnerId;
    private String emailType;
    private List<String> to;
    private String from;
    private String replyTo;
    private String subject;
    private Instant sentAt;
    private String plainTextMessage;
    private boolean isIdempotenceIdOverwritten;

    private TiSendEmailCommandBuilder() {
        super(TiSendEmailCommand::new);
    }

    public static TiSendEmailCommandBuilder create() {
        return new TiSendEmailCommandBuilder();
    }

    public TiSendEmailCommandBuilder partnerId(UUID partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public TiSendEmailCommandBuilder emailType(String emailType) {
        this.emailType = emailType;
        return this;
    }

    public TiSendEmailCommandBuilder to(List<String> to) {
        this.to = to;
        return this;
    }

    public TiSendEmailCommandBuilder from(String from) {
        this.from = from;
        return this;
    }

    public TiSendEmailCommandBuilder replyTo(String replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    public TiSendEmailCommandBuilder subject(String subject) {
        this.subject = subject;
        return this;
    }

    public TiSendEmailCommandBuilder sentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public TiSendEmailCommandBuilder plainTextMessage(String plainTextMessage) {
        this.plainTextMessage = plainTextMessage;
        return this;
    }

    @Override
    public TiSendEmailCommandBuilder idempotenceId(String idempotenceId) {
        isIdempotenceIdOverwritten = true;
        return super.idempotenceId(idempotenceId);
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getSystemName() {
        return SYSTEM_NAME;
    }

    @Override
    protected TiSendEmailCommandBuilder self() {
        return this;
    }

    @Override
    public TiSendEmailCommand build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        requireNonNull(emailType, "emailType");
        requireNonNull(to, "to");
        requireNonNull(from, "from");
        requireNonNull(replyTo, "replyTo");
        requireNonNull(subject, "subject");
        requireNonNull(sentAt, "sentAt");
        requireNonNull(plainTextMessage, "plainTextMessage");

        setPayload(
            TiSendEmailCommandPayload.newBuilder()
                .setPartnerId(partnerId)
                .setEmailType(emailType)
                .setTo(to)
                .setFrom(from)
                .setReplyTo(replyTo)
                .setSubject(subject)
                .setSentAt(sentAt)
                .setPlainTextMessage(plainTextMessage)
                .build()
        );
        return super.build();
    }

    private static void requireNonNull(Object value, String propertyName) {
        if (value == null) {
            throw AvroMessageBuilderException.propertyNull(propertyName);
        }
    }
}
