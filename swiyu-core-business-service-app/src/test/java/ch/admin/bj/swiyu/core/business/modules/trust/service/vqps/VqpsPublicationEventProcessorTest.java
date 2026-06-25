package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.vqpsJwt;
import static org.mockito.Mockito.verify;

import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsPublicationFailureReasonDto;
import ch.admin.bj.swiyu.messagetype.ti.*;
import com.nimbusds.jose.JOSEException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VqpsPublicationEventProcessorTest {

    @Mock
    private VqpsSubmissionService vqpsSubmissionService;

    @Mock
    private VqpsPublicationAwaiter vqpsPublicationAwaiter;

    @InjectMocks
    private VqpsPublicationEventProcessor processor;

    @Test
    void whenProcessSucceeded_thenMarkAsSucceededAndNotifyAwaiter()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        // GIVEN
        var submissionId = UUID.randomUUID();
        var vqpsJwt = vqpsJwt(UUID.randomUUID(), Instant.now());
        var event = new TiVqpsPublicationSucceededEvent();
        event.setPayload(
            VqpsPublicationSucceededPayload.newBuilder().setVqps(vqpsJwt).setVqpsSubmissionId(submissionId).build()
        );
        // WHEN
        processor.processVqpsPublicationSucceeded(event);
        // THEN
        verify(vqpsSubmissionService).markAsPublicationSucceeded(submissionId, vqpsJwt);
        verify(vqpsPublicationAwaiter).notifyVqpsPublicationProcessFinished(submissionId);
    }

    @Test
    void whenProcessFailed_thenMarkAsFailedAndNotifyAwaiter() {
        // GIVEN
        var submissionId = UUID.randomUUID();
        var event = new TiVqpsPublicationFailedEvent();
        var payload = new VqpsPublicationFailedPayload();
        payload.setVqpsSubmissionId(submissionId);
        payload.setFailureReason(VqpsPublicationFailureReason.UNKNOWN);
        event.setPayload(payload);

        // WHEN
        processor.processVqpsPublicationFailed(event);

        // THEN
        verify(vqpsSubmissionService).markAsPublicationFailed(submissionId, VqpsPublicationFailureReasonDto.UNKNOWN);
        verify(vqpsPublicationAwaiter).notifyVqpsPublicationProcessFinished(submissionId);
    }

    @Test
    void whenProcessSucceeded_thenNotifyAwaiterAfterMarkAsSucceeded()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var submissionId = UUID.randomUUID();
        var event = new TiVqpsPublicationSucceededEvent();
        var vqpsJwt = vqpsJwt(UUID.randomUUID(), Instant.now());
        event.setPayload(
            VqpsPublicationSucceededPayload.newBuilder().setVqps(vqpsJwt).setVqpsSubmissionId(submissionId).build()
        );
        var inOrder = Mockito.inOrder(vqpsSubmissionService, vqpsPublicationAwaiter);

        processor.processVqpsPublicationSucceeded(event);

        inOrder.verify(vqpsSubmissionService).markAsPublicationSucceeded(submissionId, vqpsJwt);
        inOrder.verify(vqpsPublicationAwaiter).notifyVqpsPublicationProcessFinished(submissionId);
    }
}
