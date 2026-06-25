package ch.admin.bj.swiyu.core.business.modules.trust.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionSubmittedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TrustAddDidSubmissionSubmittedPayload;
import java.util.UUID;

public class TiTrustAddDidSubmissionSubmittedEventBuilder
    extends AvroDomainEventBuilder<TiTrustAddDidSubmissionSubmittedEventBuilder, TiTrustAddDidSubmissionSubmittedEvent>
{

    private UUID trustAddDidSubmissionId;
    private boolean isIdempotenceIdOverwritten;

    private TiTrustAddDidSubmissionSubmittedEventBuilder() {
        super(TiTrustAddDidSubmissionSubmittedEvent::new);
    }

    public static TiTrustAddDidSubmissionSubmittedEventBuilder create() {
        return new TiTrustAddDidSubmissionSubmittedEventBuilder();
    }

    public TiTrustAddDidSubmissionSubmittedEventBuilder trustAddDidSubmissionId(UUID trustAddDidSubmissionId) {
        this.trustAddDidSubmissionId = trustAddDidSubmissionId;
        return this;
    }

    @Override
    public TiTrustAddDidSubmissionSubmittedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiTrustAddDidSubmissionSubmittedEventBuilder self() {
        return this;
    }

    @Override
    public TiTrustAddDidSubmissionSubmittedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.trustAddDidSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("trustAddDidSubmissionId");
        }
        TrustAddDidSubmissionSubmittedPayload payload = TrustAddDidSubmissionSubmittedPayload.newBuilder()
            .setTrustAddDidSubmissionId(trustAddDidSubmissionId)
            .build();
        setPayload(payload);
        return super.build();
    }
}
