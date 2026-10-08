package ch.admin.bj.swiyu.core.business.modules.trust.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerHardDeletedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerHardDeletedEvent;
import java.util.UUID;

public class TiBusinessPartnerHardDeletedEventBuilder
    extends AvroDomainEventBuilder<TiBusinessPartnerHardDeletedEventBuilder, TiBusinessPartnerHardDeletedEvent>
{

    private UUID businessPartnerId;
    private boolean isIdempotenceIdOverwritten;

    private TiBusinessPartnerHardDeletedEventBuilder() {
        super(TiBusinessPartnerHardDeletedEvent::new);
    }

    public static TiBusinessPartnerHardDeletedEventBuilder create() {
        return new TiBusinessPartnerHardDeletedEventBuilder();
    }

    public TiBusinessPartnerHardDeletedEventBuilder businessPartnerId(UUID businessPartnerId) {
        this.businessPartnerId = businessPartnerId;
        return this;
    }

    @Override
    public TiBusinessPartnerHardDeletedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiBusinessPartnerHardDeletedEventBuilder self() {
        return this;
    }

    @Override
    public TiBusinessPartnerHardDeletedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.businessPartnerId == null) {
            throw AvroMessageBuilderException.propertyNull("businessPartnerId");
        }
        BusinessPartnerHardDeletedPayload payload = BusinessPartnerHardDeletedPayload.newBuilder()
            .setBusinessPartnerId(businessPartnerId)
            .build();
        setPayload(payload);
        return super.build();
    }
}
