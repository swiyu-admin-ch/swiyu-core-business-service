package ch.admin.bj.swiyu.core.business.modules.trust.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.VqpsSubmissionAcceptedPayload;
import java.util.UUID;

public class TiVqpsSubmissionAcceptedEventBuilder
    extends AvroDomainEventBuilder<TiVqpsSubmissionAcceptedEventBuilder, TiVqpsSubmissionAcceptedEvent>
{

    private UUID vqpsSubmissionId;
    private boolean isIdempotenceIdOverwritten;

    private TiVqpsSubmissionAcceptedEventBuilder() {
        super(TiVqpsSubmissionAcceptedEvent::new);
    }

    public static TiVqpsSubmissionAcceptedEventBuilder create() {
        return new TiVqpsSubmissionAcceptedEventBuilder();
    }

    public TiVqpsSubmissionAcceptedEventBuilder vqpsSubmissionId(UUID vqpsSubmissionId) {
        this.vqpsSubmissionId = vqpsSubmissionId;
        return this;
    }

    @Override
    public TiVqpsSubmissionAcceptedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiVqpsSubmissionAcceptedEventBuilder self() {
        return this;
    }

    @Override
    public TiVqpsSubmissionAcceptedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.vqpsSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("vqpsSubmissionId");
        }
        VqpsSubmissionAcceptedPayload payload = VqpsSubmissionAcceptedPayload.newBuilder()
            .setVqpsSubmissionId(vqpsSubmissionId)
            .build();
        setPayload(payload);
        return super.build();
    }
}
