package ch.admin.bj.swiyu.core.business.modules.trust.service.protectedverification;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionApprovedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtectedVerificationSubmissionEventProcessor {

    private final ProtectedVerificationSubmissionService protectedVerificationSubmissionService;

    public void processApprovedEvent(TiProtectedVerificationSubmissionApprovedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        log.info(
            "Retrieve TiProtectedVerificationSubmissionApprovedEvent with id: {}",
            payload.getProtectedVerificationSubmissionId()
        );
        protectedVerificationSubmissionService.markAsApproved(payload.getProtectedVerificationSubmissionId());
    }

    public void processRejectedEvent(TiProtectedVerificationSubmissionRejectedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        log.info(
            "Retrieve TiProtectedVerificationSubmissionRejectedEvent with id: {}",
            payload.getProtectedVerificationSubmissionId()
        );
        protectedVerificationSubmissionService.markAsRejected(
            payload.getProtectedVerificationSubmissionId(),
            payload.getRejectReason()
        );
    }

    private static boolean isPayloadNull(AvroDomainEvent event) {
        if (event.getPayload() == null) {
            var eventId = event.getIdentity() != null ? event.getIdentity().getEventId() : null;
            log.error("Received event with eventId {} which has no payload", eventId);
            return true;
        }
        return false;
    }
}
