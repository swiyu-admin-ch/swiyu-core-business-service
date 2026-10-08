package ch.admin.bj.swiyu.core.business.modules.offboarding.infrastructure.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.messaging.avro.AvroMessagePublisher;
import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.offboarding.service.HardDeleteBusinessPartnerCommandProcessor;
import ch.admin.bj.swiyu.messagetype.ti.TiHardDeleteBusinessPartnerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class HardDeleteBusinessPartnerCommandConsumerTest {

    private final AvroMessagePublisher publisher = mock(AvroMessagePublisher.class);

    private HardDeleteBusinessPartnerCommandProcessor processor;
    private MessagingSecurityContext messagingSecurityContext;
    private HardDeleteBusinessPartnerCommandConsumer consumer;
    private TiHardDeleteBusinessPartnerCommand command;
    private Acknowledgment ack;

    @BeforeEach
    void setUp() {
        processor = mock(HardDeleteBusinessPartnerCommandProcessor.class);
        messagingSecurityContext = mock(MessagingSecurityContext.class);
        consumer = new HardDeleteBusinessPartnerCommandConsumer(processor, messagingSecurityContext);
        command = mock(TiHardDeleteBusinessPartnerCommand.class);
        ack = mock(Acknowledgment.class);
        when(command.getPublisher()).thenReturn(publisher);
    }

    @Test
    void receive_acknowledgesOnlyAfterTheDeletionWentThrough() {
        consumer.receive(command, ack);

        verify(messagingSecurityContext).setPreferredUser(publisher);
        verify(processor).process(command);
        verify(ack).acknowledge();
    }

    /** A half-finished deletion must keep the command, so that resending it resumes the run. */
    @Test
    void receive_doesNotAcknowledgeWhenTheDeletionFails() {
        doThrow(new IllegalStateException("database is gone")).when(processor).process(any());

        assertThatThrownBy(() -> consumer.receive(command, ack)).isInstanceOf(IllegalStateException.class);

        verify(ack, never()).acknowledge();
    }
}
