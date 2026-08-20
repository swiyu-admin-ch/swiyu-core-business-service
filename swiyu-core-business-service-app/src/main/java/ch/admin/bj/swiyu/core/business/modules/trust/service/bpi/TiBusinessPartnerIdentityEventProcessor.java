package ch.admin.bj.swiyu.core.business.modules.trust.service.bpi;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TiBusinessPartnerIdentityEventProcessor {

    private final BusinessPartnerService businessPartnerService;
    private final EmailCommandPublisher emailCommandPublisher;

    public void processActivatedEvent(TiBusinessPartnerIdentityActivatedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        var partnerId = UUID.fromString(payload.getBusinessPartnerIdentityId().toString());
        log.info("Processing TiBusinessPartnerIdentityActivatedEvent for partner '{}'", partnerId);

        var bpi = BusinessPartnerIdentity.builder()
            .validUntil(payload.getValidUntil())
            .trustedIdentifier(new ArrayList<>(payload.getTrustedIdentifier()))
            .status(BusinessPartnerIdentityStatus.ACTIVE)
            .lastActivated(payload.getLastActivated())
            .uid(payload.getUid())
            .entityName(new HashMap<>(payload.getEntityName()))
            .tmsVersion(payload.getVersion())
            .build();

        businessPartnerService.applyBusinessPartnerIdentity(partnerId, bpi);
    }

    public void processUpdatedEvent(TiBusinessPartnerIdentityUpdatedEvent event) {
        if (isPayloadNull(event)) {
            return;
        }
        var payload = event.getPayload();
        var partnerId = UUID.fromString(payload.getBusinessPartnerIdentityId().toString());
        log.info("Processing TiBusinessPartnerIdentityUpdatedEvent for partner '{}'", partnerId);

        var bpi = BusinessPartnerIdentity.builder()
            .validUntil(payload.getValidUntil())
            .trustedIdentifier(new ArrayList<>(payload.getTrustedIdentifier()))
            .status(toInternalStatus(payload.getStatus()))
            .lastActivated(payload.getLastActivated())
            .uid(payload.getUid())
            .entityName(new HashMap<>(payload.getEntityName()))
            .tmsVersion(payload.getVersion())
            .build();

        businessPartnerService.applyBusinessPartnerIdentity(partnerId, bpi);
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

    private BusinessPartnerIdentityStatus toInternalStatus(
        ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus avroStatus
    ) {
        return switch (avroStatus) {
            case ACTIVE -> BusinessPartnerIdentityStatus.ACTIVE;
            case DEACTIVATED -> BusinessPartnerIdentityStatus.DEACTIVATED;
        };
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
