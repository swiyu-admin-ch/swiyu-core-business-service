package ch.admin.bj.swiyu.core.business.modules.trust.service.protectedverification;

import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.messagetype.ti.ProtectedVerificationSubmissionApprovedPayload;
import ch.admin.bj.swiyu.messagetype.ti.ProtectedVerificationSubmissionRejectedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionApprovedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionRejectedEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtectedVerificationSubmissionEventProcessorTest {

    private ProtectedVerificationSubmissionService protectedVerificationSubmissionService;
    private ProtectedVerificationSubmissionEventProcessor processor;

    private final UUID submissionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        protectedVerificationSubmissionService = mock(ProtectedVerificationSubmissionService.class);
        processor = new ProtectedVerificationSubmissionEventProcessor(protectedVerificationSubmissionService);
    }

    @Test
    void processApprovedEvent_validPayload() {
        var event = mock(TiProtectedVerificationSubmissionApprovedEvent.class);
        var payload = mock(ProtectedVerificationSubmissionApprovedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getProtectedVerificationSubmissionId()).thenReturn(submissionId);

        processor.processApprovedEvent(event);

        verify(protectedVerificationSubmissionService).markAsApproved(submissionId);
    }

    @Test
    void processApprovedEvent_nullPayload() {
        var event = mock(TiProtectedVerificationSubmissionApprovedEvent.class);
        when(event.getPayload()).thenReturn(null);

        processor.processApprovedEvent(event);

        verify(protectedVerificationSubmissionService, never()).markAsApproved(any());
    }

    @Test
    void processRejectedEvent_validPayload() {
        var event = mock(TiProtectedVerificationSubmissionRejectedEvent.class);
        var payload = mock(ProtectedVerificationSubmissionRejectedPayload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getProtectedVerificationSubmissionId()).thenReturn(submissionId);
        when(payload.getRejectReason()).thenReturn("Not trusted anymore");

        processor.processRejectedEvent(event);

        verify(protectedVerificationSubmissionService).markAsRejected(submissionId, "Not trusted anymore");
    }

    @Test
    void processRejectedEvent_nullPayload() {
        var event = mock(TiProtectedVerificationSubmissionRejectedEvent.class);
        when(event.getPayload()).thenReturn(null);

        processor.processRejectedEvent(event);

        verify(protectedVerificationSubmissionService, never()).markAsRejected(any(), any());
    }
}
