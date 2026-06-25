package ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema;

import ch.admin.bj.swiyu.messagetype.ti.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VcSchemaPublicationEventProcessor {

    private final VcSchemaSubmissionService vcSchemaSubmissionService;

    public void processVcSchemaPublicationSucceeded(TiVcSchemaPublicationSucceededEvent event) {
        this.vcSchemaSubmissionService.markAsSucceeded(event.getPayload().getVcSchemaSubmissionId());
    }

    public void processVcSchemaPublicationFailed(TiVcSchemaPublicationFailedEvent event) {
        this.vcSchemaSubmissionService.markAsFailed(
            event.getPayload().getVcSchemaSubmissionId(),
            event.getPayload().getFailureReason()
        );
    }
}
