package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustAddDidEventProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrustAddDidEventConsumer {

    private final TrustAddDidEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiTrustAddDidSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiTrustAddDidSubmissionAcceptedEventListener"
    )
    public void receive(TiTrustAddDidSubmissionAcceptedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processAcceptedEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiTrustAddDidSubmissionRejectedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiTrustAddDidSubmissionRejectedEventListener"
    )
    public void receive(TiTrustAddDidSubmissionRejectedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processRejectedEvent(event);
        ack.acknowledge();
    }
}
