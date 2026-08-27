package ch.admin.bj.swiyu.core.business.modules.email.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.email.service.EmailCommandProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes the {@code TiSendEmailCommand} that this very service publishes: composing an email and
 * sending it are separate concerns that happen to live in the same service. Going through Kafka keeps
 * the send out of the transaction that triggered the notification.
 *
 * <p>No business logic here - it opens no transaction, and acknowledges only after the processor
 * returned, matching the six event consumers in {@code modules/trust}.
 */
@Component
@RequiredArgsConstructor
public class EmailCommandConsumer {

    private final EmailCommandProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(topics = { TiSendEmailCommand.TypeRef.DEFAULT_TOPIC }, id = "TiSendEmailCommandListener")
    public void receive(TiSendEmailCommand command, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(command.getPublisher());
        processor.process(command);
        ack.acknowledge();
    }
}
