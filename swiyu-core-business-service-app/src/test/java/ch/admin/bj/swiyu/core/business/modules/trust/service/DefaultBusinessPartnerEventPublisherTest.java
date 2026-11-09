package ch.admin.bj.swiyu.core.business.modules.trust.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultBusinessPartnerEventPublisherTest {

    private final DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
    private final DefaultBusinessPartnerEventPublisher publisher = new DefaultBusinessPartnerEventPublisher(
        domainEventPublisher
    );

    @Test
    void businessPartnerUpdated_publishesUpdatedEventForPartner() {
        // GIVEN
        var businessPartnerId = UUID.randomUUID();

        // WHEN
        publisher.businessPartnerUpdated(businessPartnerId);

        // THEN
        verify(domainEventPublisher).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> businessPartnerId.equals(event.getPayload().getBusinessPartnerId()))
        );
    }

    @Test
    void businessPartnerHardDeleted_publishesHardDeletedEventForPartner() {
        // GIVEN
        var businessPartnerId = UUID.randomUUID();

        // WHEN
        publisher.businessPartnerHardDeleted(businessPartnerId);

        // THEN
        verify(domainEventPublisher).publishTiBusinessPartnerHardDeletedEvent(
            argThat(event -> businessPartnerId.equals(event.getPayload().getBusinessPartnerId()))
        );
    }
}
