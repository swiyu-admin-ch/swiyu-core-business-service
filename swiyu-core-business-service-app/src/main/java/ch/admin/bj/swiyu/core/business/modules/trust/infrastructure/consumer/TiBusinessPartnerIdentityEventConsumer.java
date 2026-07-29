package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.trust.service.bpi.TiBusinessPartnerIdentityEventProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TiBusinessPartnerIdentityEventConsumer {

    private final TiBusinessPartnerIdentityEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiBusinessPartnerIdentityActivatedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiBusinessPartnerIdentityActivatedEventListener"
    )
    public void receive(TiBusinessPartnerIdentityActivatedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processActivatedEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiBusinessPartnerIdentityUpdatedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiBusinessPartnerIdentityUpdatedEventListener"
    )
    public void receive(TiBusinessPartnerIdentityUpdatedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processUpdatedEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiBusinessPartnerIdentityDeactivatedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiBusinessPartnerIdentityDeactivatedEventListener"
    )
    public void receive(TiBusinessPartnerIdentityDeactivatedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processDeactivatedEvent(event);
        ack.acknowledge();
    }
}
