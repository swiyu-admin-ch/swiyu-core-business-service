package ch.admin.bj.swiyu.core.business.modules.payment.service;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.messagetype.ti.TiPaymentNotificationReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentChangeNotificationEventProcessor {

    private final PaymentService paymentService;

    @Transactional
    @IdempotentMessageHandler
    public void process(TiPaymentNotificationReceivedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        var paymentId = java.util.UUID.fromString(payload.getPaymentId().toString());
        log.info("Processing TiPaymentNotificationReceivedEvent for payment '{}'", paymentId);
        paymentService.finalizePayment(paymentId);
    }

    private static boolean isPayloadNull(AvroDomainEvent event) {
        if (event.getPayload() == null) {
            var eventId = event.getIdentity() != null ? event.getIdentity().getEventId() : null;
            log.error("Received PaymentNotification event with eventId {} which has no payload", eventId);
            return true;
        }
        return false;
    }
}
