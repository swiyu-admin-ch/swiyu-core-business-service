package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingInformationRequestedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingRejectedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrustOnboardingEventProcessor {

    private final TrustOnboardingService trustOnboardingService;

    public void processInformationRequestedEvent(TiTrustOnboardingInformationRequestedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        log.info(
            "Retrieve TiTrustOnboardingInformationRequestedEvent with id: {}",
            payload.getTrustOnboardingSubmissionId()
        );
        this.trustOnboardingService.markAsInformationRequested(
            payload.getTrustOnboardingSubmissionId(),
            payload.getRejectReason(),
            payload.getPartnerNote()
        );
    }

    public void processOnboardingRequestRejectedEvent(TiTrustOnboardingRejectedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        log.info(
            "Retrieve TiTrustOnboardingRejectedEvent with id: {}",
            event.getPayload().getTrustOnboardingSubmissionId()
        );
        var payload = event.getPayload();
        this.trustOnboardingService.markAsRejected(payload.getTrustOnboardingSubmissionId(), payload.getRejectReason());
    }

    public void processOnboardingRequestAcceptedEvent(TiTrustOnboardingSucceededEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        log.info(
            "Retrieve TiTrustOnboardingSucceededEvent with id: {}",
            event.getPayload().getTrustOnboardingSubmissionId()
        );
        this.trustOnboardingService.markAsSucceeded(event.getPayload().getTrustOnboardingSubmissionId());
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
