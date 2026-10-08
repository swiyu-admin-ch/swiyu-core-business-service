package ch.admin.bj.swiyu.core.business.modules.offboarding.infrastructure.consumer;

import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.offboarding.service.HardDeleteBusinessPartnerCommandProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiHardDeleteBusinessPartnerCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HardDeleteBusinessPartnerCommandConsumer {

    private final HardDeleteBusinessPartnerCommandProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiHardDeleteBusinessPartnerCommand.TypeRef.DEFAULT_TOPIC },
        id = "TiHardDeleteBusinessPartnerCommandListener"
    )
    public void receive(TiHardDeleteBusinessPartnerCommand command, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(command.getPublisher());
        processor.process(command);
        ack.acknowledge();
    }
}
