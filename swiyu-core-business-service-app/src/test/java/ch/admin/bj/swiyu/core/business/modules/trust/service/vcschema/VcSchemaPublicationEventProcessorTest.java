package ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema;

import ch.admin.bj.swiyu.messagetype.ti.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VcSchemaPublicationEventProcessorTest {

    @Mock
    private VcSchemaSubmissionService vcSchemaSubmissionService;

    @InjectMocks
    private VcSchemaPublicationEventProcessor processor;

    @Test
    void whenProcessSucceeded_thenMarkAsSucceededCalled() {
        // Arrange
        var submissionId = UUID.randomUUID();
        var event = new TiVcSchemaPublicationSucceededEvent();
        var payload = VcSchemaPublicationSucceededPayload.newBuilder().setVcSchemaSubmissionId(submissionId).build();
        event.setPayload(payload);

        // Act
        processor.processVcSchemaPublicationSucceeded(event);

        // Assert
        Mockito.verify(vcSchemaSubmissionService, Mockito.times(1)).markAsSucceeded(submissionId);
    }

    @Test
    void whenProcessFailed_thenMarkAsFailedCalled() {
        // Arrange
        var submissionId = UUID.randomUUID();
        var event = new TiVcSchemaPublicationFailedEvent();
        var payload = new VcSchemaPublicationFailedPayload();
        payload.setVcSchemaSubmissionId(submissionId);
        payload.setFailureReason("oops something went wrong");
        event.setPayload(payload);

        // Act
        processor.processVcSchemaPublicationFailed(event);

        // Assert
        Mockito.verify(vcSchemaSubmissionService, Mockito.times(1)).markAsFailed(
            submissionId,
            "oops something went wrong"
        );
    }
}
