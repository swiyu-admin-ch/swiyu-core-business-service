package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.vqpsJwt;
import static org.mockito.Mockito.verify;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsPublicationFailureReasonDto;
import ch.admin.bj.swiyu.messagetype.ti.*;
import com.nimbusds.jose.JOSEException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VqpsPublicationEventProcessorTest {

    @Mock
    private VqpsSubmissionService vqpsSubmissionService;

    @Mock
    private VqpsPublicationAwaiter vqpsPublicationAwaiter;

    @InjectMocks
    private VqpsPublicationEventProcessor processor;

    @BeforeAll
    static void installAvroClassWhitelist() {
        AvroClassSecurity.installDefaultIfMissing();
    }

    @Test
    void processVqpsPublicationSucceeded()
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
    }

    @Test
    void processVqpsPublicationFailed() {
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
    }
}
