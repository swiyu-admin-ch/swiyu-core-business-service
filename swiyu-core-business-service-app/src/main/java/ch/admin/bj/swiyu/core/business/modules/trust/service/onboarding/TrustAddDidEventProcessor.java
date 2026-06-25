package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrustAddDidEventProcessor {

    private final TrustAdditionalDidsService trustAdditionalDidsService;

    public void processAcceptedEvent(TiTrustAddDidSubmissionAcceptedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        log.info(
            "Retrieve TiTrustAddDidSubmissionAcceptedEvent with id: {}",
            event.getPayload().getTrustAddDidSubmissionId()
        );
        this.trustAdditionalDidsService.markAsSucceeded(event.getPayload().getTrustAddDidSubmissionId());
    }

    public void processRejectedEvent(TiTrustAddDidSubmissionRejectedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        log.info("Retrieve TiTrustAddDidSubmissionRejectedEvent with id: {}", payload.getTrustAddDidSubmissionId());
        this.trustAdditionalDidsService.markAsRejected(payload.getTrustAddDidSubmissionId(), payload.getRejectReason());
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
