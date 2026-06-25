package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vqps.VqpsPublicationEventProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsPublicationFailedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsPublicationSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VqpsPublicationEventConsumer {

    private final VqpsPublicationEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiVqpsPublicationSucceededEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiVqpsPublicationSucceededEventListener"
    )
    public void receive(TiVqpsPublicationSucceededEvent event, Acknowledgment ack) {
        log.debug(
            "Received TiVqpsPublicationSucceededEvent for submission id {}",
            event.getPayload().getVqpsSubmissionId()
        );
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processVqpsPublicationSucceeded(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiVqpsPublicationFailedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiVqpsPublicationFailedEventListener"
    )
    public void receive(TiVqpsPublicationFailedEvent event, Acknowledgment ack) {
        log.debug(
            "Received TiVqpsPublicationFailedEvent for submission id {}",
            event.getPayload().getVqpsSubmissionId()
        );
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processVqpsPublicationFailed(event);
        ack.acknowledge();
    }
}
