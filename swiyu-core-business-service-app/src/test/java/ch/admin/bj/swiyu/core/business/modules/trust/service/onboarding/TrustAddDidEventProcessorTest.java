package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.messagetype.ti.*;
import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrustAddDidEventProcessorTest {

    private TrustAdditionalDidsService trustAdditionalDidsService;
    private TrustAddDidEventProcessor processor;

    private final UUID submissionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        trustAdditionalDidsService = mock(TrustAdditionalDidsService.class);
        processor = new TrustAddDidEventProcessor(trustAdditionalDidsService);
    }

    @Test
    void testProcessAcceptedEvent_validPayload() {
        var event = mock(TiTrustAddDidSubmissionAcceptedEvent.class);
        var payload = mock(TrustAddDidSubmissionAcceptedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustAddDidSubmissionId()).thenReturn(submissionId);

        processor.processAcceptedEvent(event);

        verify(trustAdditionalDidsService).markAsSucceeded(submissionId);
    }

    @Test
    void testProcessAcceptedEvent_nullPayload() {
        var event = mock(TiTrustAddDidSubmissionAcceptedEvent.class);
        when(event.getPayload()).thenReturn(null);

        processor.processAcceptedEvent(event);

        verify(trustAdditionalDidsService, never()).markAsSucceeded(any());
    }

    @Test
    void testProcessAcceptedEvent_nullSubmissionId() {
        var event = mock(TiTrustAddDidSubmissionAcceptedEvent.class);
        var payload = mock(TrustAddDidSubmissionAcceptedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustAddDidSubmissionId()).thenReturn(null);

        processor.processAcceptedEvent(event);

        verify(trustAdditionalDidsService).markAsSucceeded(null);
    }

    @Test
    void testProcessRejectedEvent_validPayload() {
        var event = mock(TiTrustAddDidSubmissionRejectedEvent.class);
        var payload = mock(TrustAddDidSubmissionRejectedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustAddDidSubmissionId()).thenReturn(submissionId);
        when(payload.getRejectReason()).thenReturn(RejectReason.UNKNOWN);

        processor.processRejectedEvent(event);

        verify(trustAdditionalDidsService).markAsRejected(submissionId, RejectReason.UNKNOWN);
    }

    @Test
    void testProcessRejectedEvent_nullPayload() {
        var event = mock(TiTrustAddDidSubmissionRejectedEvent.class);
        when(event.getPayload()).thenReturn(null);

        processor.processRejectedEvent(event);

        verify(trustAdditionalDidsService, never()).markAsRejected(any(), any());
    }

    @Test
    void testProcessRejectedEvent_nullSubmissionId() {
        var event = mock(TiTrustAddDidSubmissionRejectedEvent.class);
        var payload = mock(TrustAddDidSubmissionRejectedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getTrustAddDidSubmissionId()).thenReturn(null);
        when(payload.getRejectReason()).thenReturn(null);

        processor.processRejectedEvent(event);

        verify(trustAdditionalDidsService).markAsRejected(null, null);
    }
}
