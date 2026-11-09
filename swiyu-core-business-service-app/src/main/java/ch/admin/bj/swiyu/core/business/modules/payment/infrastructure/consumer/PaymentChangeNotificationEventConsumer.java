package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentChangeNotificationEventProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiPaymentChangeNotificationReceivedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentChangeNotificationEventConsumer {

    private final PaymentChangeNotificationEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(topics = { TiPaymentChangeNotificationReceivedEvent.TypeRef.DEFAULT_TOPIC })
    public void receive(TiPaymentChangeNotificationReceivedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.process(event);
        ack.acknowledge();
    }
}
