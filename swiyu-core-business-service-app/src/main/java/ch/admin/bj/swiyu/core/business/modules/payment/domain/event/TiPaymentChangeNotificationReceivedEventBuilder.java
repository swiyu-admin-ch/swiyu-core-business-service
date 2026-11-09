package ch.admin.bj.swiyu.core.business.modules.payment.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.PaymentChangeNotificationReceivedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiPaymentChangeNotificationReceivedEvent;
import java.util.UUID;

public class TiPaymentChangeNotificationReceivedEventBuilder
    extends AvroDomainEventBuilder<
        TiPaymentChangeNotificationReceivedEventBuilder,
        TiPaymentChangeNotificationReceivedEvent
    >
{

    private UUID paymentId;
    private boolean isIdempotenceIdOverwritten = false;

    private TiPaymentChangeNotificationReceivedEventBuilder() {
        super(TiPaymentChangeNotificationReceivedEvent::new);
    }

    public static TiPaymentChangeNotificationReceivedEventBuilder create() {
        return new TiPaymentChangeNotificationReceivedEventBuilder();
    }

    public TiPaymentChangeNotificationReceivedEventBuilder paymentId(UUID paymentId) {
        this.paymentId = paymentId;
        return this;
    }

    @Override
    public TiPaymentChangeNotificationReceivedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiPaymentChangeNotificationReceivedEventBuilder self() {
        return this;
    }

    @Override
    public TiPaymentChangeNotificationReceivedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.paymentId == null) {
            throw AvroMessageBuilderException.propertyNull("payload.paymentId");
        }
        PaymentChangeNotificationReceivedPayload declarationPayload =
            PaymentChangeNotificationReceivedPayload.newBuilder().setPaymentId(paymentId).build();
        setPayload(declarationPayload);
        return super.build();
    }
}
