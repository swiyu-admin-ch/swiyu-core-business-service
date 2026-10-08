package ch.admin.bj.swiyu.core.business.modules.offboarding.service;

import ch.admin.bj.swiyu.core.business.common.audit.AuditTrigger;
import ch.admin.bj.swiyu.messagetype.ti.TiHardDeleteBusinessPartnerCommand;
import ch.admin.bj.swiyu.messagetype.ti.common.AuditSystemComponent;
import ch.admin.bj.swiyu.messagetype.ti.common.AuditUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Deliberately not {@code @Transactional} and not {@code @IdempotentMessageHandler}: the deletion spans
 * three databases and manages its own transactions, and a command redelivered after a partial run has
 * to finish that run, not be skipped.
 *
 * <p>Failures are not classified here. They propagate like in every other processor of this service and
 * end up in the error handling service, from where ops resends the command - after arming the safeguard,
 * if that was the reason. Declaring them temporary would claim knowledge this class does not have: a
 * disarmed safeguard needs a human, and an exception from three datasources can be either.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HardDeleteBusinessPartnerCommandProcessor {

    private final OffboardingService offboardingService;

    public void process(TiHardDeleteBusinessPartnerCommand command) {
        var payload = command.getPayload();
        var businessPartnerId = payload.getBusinessPartnerId();
        log.info("Processing TiHardDeleteBusinessPartnerCommand for partner '{}'", businessPartnerId);
        offboardingService.hardDeleteBusinessPartner(businessPartnerId, toAuditTrigger(payload.getTrigger()));
    }

    /** Fails loudly on an unknown union branch: the audit record must not name the wrong originator. */
    private static AuditTrigger toAuditTrigger(Object trigger) {
        return switch (trigger) {
            case AuditUser user -> new AuditTrigger.User(user.getId(), user.getIdentityProvider());
            case AuditSystemComponent component -> new AuditTrigger.SystemComponent(
                component.getDepartment(),
                component.getSystem(),
                component.getComponent()
            );
            case null -> throw new IllegalArgumentException(
                "TiHardDeleteBusinessPartnerCommand has no trigger, cannot audit the deletion"
            );
            default -> throw new IllegalArgumentException(
                "Unsupported trigger type in TiHardDeleteBusinessPartnerCommand: " + trigger.getClass().getName()
            );
        };
    }
}
