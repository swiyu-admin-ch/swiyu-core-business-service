package ch.admin.bj.swiyu.core.business.modules.trust.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.ProtectedVerificationSubmissionAcceptedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionAcceptedEvent;
import java.util.UUID;

public class TiProtectedVerificationSubmissionAcceptedEventBuilder
    extends AvroDomainEventBuilder<
        TiProtectedVerificationSubmissionAcceptedEventBuilder,
        TiProtectedVerificationSubmissionAcceptedEvent
    >
{

    private UUID protectedVerificationSubmissionId;
    private boolean isIdempotenceIdOverwritten;

    private TiProtectedVerificationSubmissionAcceptedEventBuilder() {
        super(TiProtectedVerificationSubmissionAcceptedEvent::new);
    }

    public static TiProtectedVerificationSubmissionAcceptedEventBuilder create() {
        return new TiProtectedVerificationSubmissionAcceptedEventBuilder();
    }

    public TiProtectedVerificationSubmissionAcceptedEventBuilder protectedVerificationSubmissionId(
        UUID protectedVerificationSubmissionId
    ) {
        this.protectedVerificationSubmissionId = protectedVerificationSubmissionId;
        return this;
    }

    @Override
    public TiProtectedVerificationSubmissionAcceptedEventBuilder idempotenceId(String idempotenceId) {
        isIdempotenceIdOverwritten = true;
        return super.idempotenceId(idempotenceId);
    }

    @Override
    protected String getServiceName() {
        return EventBuilderProperties.SERVICE_NAME;
    }

    @Override
    protected String getSystemName() {
        return EventBuilderProperties.SYSTEM_NAME;
    }

    @Override
    protected TiProtectedVerificationSubmissionAcceptedEventBuilder self() {
        return this;
    }

    @Override
    public TiProtectedVerificationSubmissionAcceptedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.protectedVerificationSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("protectedVerificationSubmissionId");
        }
        ProtectedVerificationSubmissionAcceptedPayload payload =
            ProtectedVerificationSubmissionAcceptedPayload.newBuilder()
                .setProtectedVerificationSubmissionId(protectedVerificationSubmissionId)
                .build();
        setPayload(payload);
        return super.build();
    }
}
