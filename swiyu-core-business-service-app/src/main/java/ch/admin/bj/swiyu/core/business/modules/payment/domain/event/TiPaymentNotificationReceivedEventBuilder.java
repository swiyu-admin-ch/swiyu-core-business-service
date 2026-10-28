package ch.admin.bj.swiyu.core.business.modules.payment.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.PaymentNotificationReceivedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiPaymentNotificationReceivedEvent;
import java.util.UUID;

public class TiPaymentNotificationReceivedEventBuilder
    extends AvroDomainEventBuilder<TiPaymentNotificationReceivedEventBuilder, TiPaymentNotificationReceivedEvent>
{

    private UUID paymentId;
    private boolean isIdempotenceIdOverwritten = false;

    private TiPaymentNotificationReceivedEventBuilder() {
        super(TiPaymentNotificationReceivedEvent::new);
    }

    public static TiPaymentNotificationReceivedEventBuilder create() {
        return new TiPaymentNotificationReceivedEventBuilder();
    }

    public TiPaymentNotificationReceivedEventBuilder paymentId(UUID paymentId) {
        this.paymentId = paymentId;
        return this;
    }

    @Override
    public TiPaymentNotificationReceivedEventBuilder idempotenceId(String idempotenceId) {
        isIdempotenceIdOverwritten = true;
        return super.idempotenceId(idempotenceId);
    }

    @Override
    protected String getServiceName() {
        return "swiyu-core-business-service";
    }

    @Override
    protected String getSystemName() {
        return "ti";
    }

    @Override
    protected TiPaymentNotificationReceivedEventBuilder self() {
        return this;
    }

    @Override
    public TiPaymentNotificationReceivedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.paymentId == null) {
            throw AvroMessageBuilderException.propertyNull("payload.paymentId");
        }
        PaymentNotificationReceivedPayload declarationPayload = PaymentNotificationReceivedPayload.newBuilder()
            .setPaymentId(paymentId)
            .build();
        setPayload(declarationPayload);
        return super.build();
    }
}
