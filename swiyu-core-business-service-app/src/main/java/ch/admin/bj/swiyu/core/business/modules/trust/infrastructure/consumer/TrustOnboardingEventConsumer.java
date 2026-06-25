package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingEventProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingInformationRequestedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingRejectedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrustOnboardingEventConsumer {

    private final TrustOnboardingEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiTrustOnboardingSucceededEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiTrustOnboardingSucceededEventListener"
    )
    public void receive(TiTrustOnboardingSucceededEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processOnboardingRequestAcceptedEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiTrustOnboardingRejectedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiTrustOnboardingRejectedEventListener"
    )
    public void receive(TiTrustOnboardingRejectedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processOnboardingRequestRejectedEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiTrustOnboardingInformationRequestedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiTrustOnboardingInformationRequestedEventListener"
    )
    public void receive(TiTrustOnboardingInformationRequestedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processInformationRequestedEvent(event);
        ack.acknowledge();
    }
}
