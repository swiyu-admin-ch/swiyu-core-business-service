package ch.admin.bj.swiyu.core.business.modules.payment.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.PaymentSucceededPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiPaymentSucceededEvent;
import java.util.UUID;

public class TiPaymentSucceededEventBuilder
    extends AvroDomainEventBuilder<TiPaymentSucceededEventBuilder, TiPaymentSucceededEvent>
{

    private UUID paymentId;
    private boolean isIdempotenceIdOverwritten = false;

    private TiPaymentSucceededEventBuilder() {
        super(TiPaymentSucceededEvent::new);
    }

    public static TiPaymentSucceededEventBuilder create() {
        return new TiPaymentSucceededEventBuilder();
    }

    public TiPaymentSucceededEventBuilder paymentId(UUID paymentId) {
        this.paymentId = paymentId;
        return this;
    }

    @Override
    public TiPaymentSucceededEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiPaymentSucceededEventBuilder self() {
        return this;
    }

    @Override
    public TiPaymentSucceededEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.paymentId == null) {
            throw AvroMessageBuilderException.propertyNull("payload.paymentId");
        }
        PaymentSucceededPayload payload = PaymentSucceededPayload.newBuilder().setPaymentId(paymentId).build();
        setPayload(payload);
        return super.build();
    }
}
