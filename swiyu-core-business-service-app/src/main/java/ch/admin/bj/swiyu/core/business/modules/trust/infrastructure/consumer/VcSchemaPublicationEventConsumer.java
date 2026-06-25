package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema.VcSchemaPublicationEventProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaPublicationFailedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaPublicationSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VcSchemaPublicationEventConsumer {

    private final VcSchemaPublicationEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiVcSchemaPublicationSucceededEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiVcSchemaPublicationSucceededEventListener"
    )
    public void receive(TiVcSchemaPublicationSucceededEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processVcSchemaPublicationSucceeded(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiVcSchemaPublicationFailedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiVcSchemaPublicationFailedEventListener"
    )
    public void receive(TiVcSchemaPublicationFailedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processVcSchemaPublicationFailed(event);
        ack.acknowledge();
    }
}
