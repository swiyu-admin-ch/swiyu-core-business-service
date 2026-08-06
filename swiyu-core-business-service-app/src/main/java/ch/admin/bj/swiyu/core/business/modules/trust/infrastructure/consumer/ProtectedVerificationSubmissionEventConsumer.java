package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.trust.service.protectedverification.ProtectedVerificationSubmissionEventProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionApprovedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProtectedVerificationSubmissionEventConsumer {

    private final ProtectedVerificationSubmissionEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiProtectedVerificationSubmissionApprovedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiProtectedVerificationSubmissionApprovedEventListener"
    )
    public void receive(TiProtectedVerificationSubmissionApprovedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processApprovedEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiProtectedVerificationSubmissionRejectedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiProtectedVerificationSubmissionRejectedEventListener"
    )
    public void receive(TiProtectedVerificationSubmissionRejectedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processRejectedEvent(event);
        ack.acknowledge();
    }
}
