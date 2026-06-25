package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.messagetype.ti.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrustOnboardingEventProcessorTest {

    private TrustOnboardingService trustOnboardingService;
    private TrustOnboardingEventProcessor processor;

    private final UUID submissionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        trustOnboardingService = mock(TrustOnboardingService.class);
        processor = new TrustOnboardingEventProcessor(trustOnboardingService);
    }

    @Test
    void testProcessInformationRequestedEvent_validPayload() {
        var event = mock(TiTrustOnboardingInformationRequestedEvent.class);
        var payload = mock(TrustOnboardingInformationRequestedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustOnboardingSubmissionId()).thenReturn(submissionId);

        processor.processInformationRequestedEvent(event);

        verify(trustOnboardingService).markAsInformationRequested(
            submissionId,
            event.getPayload().getRejectReason(),
            event.getPayload().getPartnerNote()
        );
    }

    @Test
    void testProcessInformationRequestedEvent_nullPayload() {
        var event = mock(TiTrustOnboardingInformationRequestedEvent.class);
        when(event.getPayload()).thenReturn(null);

        processor.processInformationRequestedEvent(event);

        verify(trustOnboardingService, never()).markAsInformationRequested(any(), any(), any());
    }

    @Test
    void testProcessInformationRequestedEvent_nullSubmissionId() {
        var event = mock(TiTrustOnboardingInformationRequestedEvent.class);
        var payload = mock(TrustOnboardingInformationRequestedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustOnboardingSubmissionId()).thenReturn(null);

        processor.processInformationRequestedEvent(event);

        verify(trustOnboardingService).markAsInformationRequested(
            null,
            event.getPayload().getRejectReason(),
            event.getPayload().getPartnerNote()
        );
    }

    @Test
    void testProcessRejectedEvent_validPayload() {
        var event = mock(TiTrustOnboardingRejectedEvent.class);
        var payload = mock(TrustOnboardingRejectedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustOnboardingSubmissionId()).thenReturn(submissionId);

        processor.processOnboardingRequestRejectedEvent(event);

        verify(trustOnboardingService).markAsRejected(submissionId, event.getPayload().getRejectReason());
    }

    @Test
    void testProcessRejectedEvent_nullPayload() {
        var event = mock(TiTrustOnboardingRejectedEvent.class);
        when(event.getPayload()).thenReturn(null);

        processor.processOnboardingRequestRejectedEvent(event);

        verify(trustOnboardingService, never()).markAsRejected(any(), any());
    }

    @Test
    void testProcessRejectedEvent_nullSubmissionId() {
        var event = mock(TiTrustOnboardingRejectedEvent.class);
        var payload = mock(TrustOnboardingRejectedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustOnboardingSubmissionId()).thenReturn(null);

        processor.processOnboardingRequestRejectedEvent(event);

        verify(trustOnboardingService).markAsRejected(null, event.getPayload().getRejectReason());
    }

    @Test
    void testProcessSucceededEvent_validPayload() {
        var event = mock(TiTrustOnboardingSucceededEvent.class);
        var payload = mock(TrustOnboardingSucceededPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustOnboardingSubmissionId()).thenReturn(submissionId);

        processor.processOnboardingRequestAcceptedEvent(event);

        verify(trustOnboardingService).markAsSucceeded(submissionId);
    }

    @Test
    void testProcessSucceededEvent_nullPayload() {
        var event = mock(TiTrustOnboardingSucceededEvent.class);
        when(event.getPayload()).thenReturn(null);

        processor.processOnboardingRequestAcceptedEvent(event);

        verify(trustOnboardingService, never()).markAsSucceeded(any());
    }

    @Test
    void testProcessSucceededEvent_nullSubmissionId() {
        var event = mock(TiTrustOnboardingSucceededEvent.class);
        var payload = mock(TrustOnboardingSucceededPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustOnboardingSubmissionId()).thenReturn(null);

        processor.processOnboardingRequestAcceptedEvent(event);

        verify(trustOnboardingService).markAsSucceeded(null);
    }
}
