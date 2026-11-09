package ch.admin.bj.swiyu.core.business.modules.trust.service;

import ch.admin.bj.swiyu.core.business.common.trust.BusinessPartnerEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.event.TiBusinessPartnerHardDeletedEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.event.TiBusinessPartnerUpdatedEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes the {@code TiBusinessPartner} change notifications through the trust module's
 * {@link DomainEventPublisher}, i.e. through the transactional outbox.
 *
 * <p>Both methods declare {@link Propagation#MANDATORY} so the outbox contract is enforced at the
 * port boundary as well: an event must not be recorded without the surrounding state change.
 */
@Component
@AllArgsConstructor
public class DefaultBusinessPartnerEventPublisher implements BusinessPartnerEventPublisher {

    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void businessPartnerUpdated(UUID businessPartnerId) {
        domainEventPublisher.publishTiBusinessPartnerUpdatedEvent(
            TiBusinessPartnerUpdatedEventBuilder.create().businessPartnerId(businessPartnerId).build()
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void businessPartnerHardDeleted(UUID businessPartnerId) {
        domainEventPublisher.publishTiBusinessPartnerHardDeletedEvent(
            TiBusinessPartnerHardDeletedEventBuilder.create().businessPartnerId(businessPartnerId).build()
        );
    }
}
