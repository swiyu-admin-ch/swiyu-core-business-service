package ch.admin.bj.swiyu.core.business.modules.management.service;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessPartnerIdentityEventProcessor {

    private final BusinessPartnerService businessPartnerService;
    private final EmailCommandPublisher emailCommandPublisher;

    @Transactional
    @IdempotentMessageHandler
    public void processActivatedEvent(TiBusinessPartnerIdentityActivatedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        var partnerId = UUID.fromString(payload.getBusinessPartnerIdentityId().toString());
        log.info("Processing TiBusinessPartnerIdentityActivatedEvent for partner '{}'", partnerId);
        businessPartnerService.applyActivatedBusinessPartnerIdentity(partnerId, payload);
    }

    @Transactional
    @IdempotentMessageHandler
    public void processUpdatedEvent(TiBusinessPartnerIdentityUpdatedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        var partnerId = UUID.fromString(payload.getBusinessPartnerIdentityId().toString());
        log.info("Processing TiBusinessPartnerIdentityUpdatedEvent for partner '{}'", partnerId);
        businessPartnerService.applyUpdatedBusinessPartnerIdentity(partnerId, payload);
    }

    @Transactional
    @IdempotentMessageHandler
    public void processDeactivatedEvent(TiBusinessPartnerIdentityDeactivatedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        var partnerId = UUID.fromString(payload.getBusinessPartnerIdentityId().toString());
        log.info("Processing TiBusinessPartnerIdentityDeactivatedEvent for partner '{}'", partnerId);

        if (businessPartnerService.deactivateBusinessPartnerIdentity(partnerId, payload.getVersion())) {
            emailCommandPublisher.trustIdentityExpired(partnerId);
        } else {
            log.warn(
                "Received deactivation event for partner '{}' but no BusinessPartnerIdentity exists. Ignoring.",
                partnerId
            );
        }
    }

    private static boolean isPayloadNull(AvroDomainEvent event) {
        if (event.getPayload() == null) {
            var eventId = event.getIdentity() != null ? event.getIdentity().getEventId() : null;
            log.error("Received BPI event with eventId {} which has no payload", eventId);
            return true;
        }
        return false;
    }
}
