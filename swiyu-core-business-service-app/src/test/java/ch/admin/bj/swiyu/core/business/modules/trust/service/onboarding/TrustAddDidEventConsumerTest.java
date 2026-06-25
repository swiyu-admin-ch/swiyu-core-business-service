package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer.TrustAddDidEventConsumer;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionRejectedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class TrustAddDidEventConsumerTest {

    private TrustAddDidEventProcessor processor;
    private MessagingSecurityContext messagingSecurityContext;
    private TrustAddDidEventConsumer consumer;

    private final AvroDomainEventPublisher publisher = mock(AvroDomainEventPublisher.class);

    @BeforeEach
    void setUp() {
        processor = mock(TrustAddDidEventProcessor.class);
        messagingSecurityContext = mock(MessagingSecurityContext.class);
        consumer = new TrustAddDidEventConsumer(processor, messagingSecurityContext);
    }

    @Test
    void testReceiveAcceptedEvent() {
        TiTrustAddDidSubmissionAcceptedEvent event = mock(TiTrustAddDidSubmissionAcceptedEvent.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        when(event.getPublisher()).thenReturn(publisher);

        consumer.receive(event, ack);

        verify(messagingSecurityContext).setPreferredUser(publisher);
        verify(processor).processAcceptedEvent(event);
        verify(ack).acknowledge();
    }

    @Test
    void testReceiveRejectedEvent() {
        TiTrustAddDidSubmissionRejectedEvent event = mock(TiTrustAddDidSubmissionRejectedEvent.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        when(event.getPublisher()).thenReturn(publisher);

        consumer.receive(event, ack);

        verify(messagingSecurityContext).setPreferredUser(publisher);
        verify(processor).processRejectedEvent(event);
        verify(ack).acknowledge();
    }
}
