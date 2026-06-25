package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bj.swiyu.core.business.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer.TrustOnboardingEventConsumer;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingInformationRequestedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingRejectedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSucceededEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class TrustOnboardingEventConsumerTest {

    private TrustOnboardingEventProcessor processor;
    private MessagingSecurityContext messagingSecurityContext;
    private TrustOnboardingEventConsumer consumer;

    private final AvroDomainEventPublisher publisher = mock(AvroDomainEventPublisher.class);

    @BeforeEach
    void setUp() {
        processor = mock(TrustOnboardingEventProcessor.class);
        messagingSecurityContext = mock(MessagingSecurityContext.class);
        consumer = new TrustOnboardingEventConsumer(processor, messagingSecurityContext);
    }

    @Test
    void testReceiveSucceededEvent() {
        TiTrustOnboardingSucceededEvent event = mock(TiTrustOnboardingSucceededEvent.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        when(event.getPublisher()).thenReturn(publisher);

        consumer.receive(event, ack);

        verify(messagingSecurityContext).setPreferredUser(publisher);
        verify(processor).processOnboardingRequestAcceptedEvent(event);
        verify(ack).acknowledge();
    }

    @Test
    void testReceiveRejectedEvent() {
        TiTrustOnboardingRejectedEvent event = mock(TiTrustOnboardingRejectedEvent.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        when(event.getPublisher()).thenReturn(publisher);

        consumer.receive(event, ack);

        verify(messagingSecurityContext).setPreferredUser(publisher);
        verify(processor).processOnboardingRequestRejectedEvent(event);
        verify(ack).acknowledge();
    }

    @Test
    void testReceiveInformationRequestedEvent() {
        TiTrustOnboardingInformationRequestedEvent event = mock(TiTrustOnboardingInformationRequestedEvent.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        when(event.getPublisher()).thenReturn(publisher);

        consumer.receive(event, ack);

        verify(messagingSecurityContext).setPreferredUser(publisher);
        verify(processor).processInformationRequestedEvent(event);
        verify(ack).acknowledge();
    }
}
