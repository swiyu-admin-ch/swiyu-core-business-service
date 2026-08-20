package ch.admin.bj.swiyu.core.business.modules.trust.service.bpi;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.activeBusinessPartnerIdentity;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeGov;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityDeactivatedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The "identity expired" email is triggered here rather than inside {@code BusinessPartnerService}:
 * the publisher resolves the recipient through that very service, so publishing from within it would
 * close a bean cycle. The service only reports whether there was an identity to deactivate.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "Test")
class TiBusinessPartnerIdentityEventProcessorIT {

    @MockitoBean
    EmailCommandPublisher emailCommandPublisher;

    @MockitoBean
    AuditPublisher auditPublisher;

    @MockitoBean
    PamsClient pamsClient;

    @Autowired
    TiBusinessPartnerIdentityEventProcessor processor;

    @Autowired
    TestRepositories repos;

    @BeforeEach
    void setUp() {
        repos.truncateTables();
    }

    @Test
    void notifiesThePartnerWhenTheIdentityWasDeactivated() {
        var partnerId = givenPartnerWithActiveIdentity();

        processor.processDeactivatedEvent(deactivatedEvent(partnerId));

        assertThat(identityStatusOf(partnerId)).isEqualTo(BusinessPartnerIdentityStatus.DEACTIVATED);
        verify(emailCommandPublisher).trustIdentityExpired(partnerId);
    }

    @Test
    void sendsNoEmailWhenThereWasNoIdentityToDeactivate() {
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));

        processor.processDeactivatedEvent(deactivatedEvent(partner.getId()));

        verifyNoInteractions(emailCommandPublisher);
    }

    @Test
    void processesARedeliveredEventOnlyOnce() {
        var partnerId = givenPartnerWithActiveIdentity();
        var event = deactivatedEvent(partnerId, UUID.randomUUID().toString());

        processor.processDeactivatedEvent(event);
        processor.processDeactivatedEvent(event);

        verify(emailCommandPublisher).trustIdentityExpired(partnerId);
    }

    private UUID givenPartnerWithActiveIdentity() {
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        partner.applyBusinessPartnerIdentityEvent(activeBusinessPartnerIdentity());
        return repos.businessPartner.save(partner).getId();
    }

    private BusinessPartnerIdentityStatus identityStatusOf(UUID partnerId) {
        return repos.businessPartner.findById(partnerId).orElseThrow().getBusinessPartnerIdentity().getStatus();
    }

    private static TiBusinessPartnerIdentityDeactivatedEvent deactivatedEvent(UUID partnerId) {
        return deactivatedEvent(partnerId, UUID.randomUUID().toString());
    }

    /**
     * The idempotence aspect only reads the idempotence id and the message type name, so a stub is
     * enough here and avoids depending on the full Avro message envelope.
     */
    private static TiBusinessPartnerIdentityDeactivatedEvent deactivatedEvent(UUID partnerId, String idempotenceId) {
        var identity = mock(AvroDomainEventIdentity.class);
        when(identity.getIdempotenceId()).thenReturn(idempotenceId);

        var type = mock(AvroDomainEventType.class);
        when(type.getName()).thenReturn("TiBusinessPartnerIdentityDeactivatedEvent");

        var payload = new BusinessPartnerIdentityDeactivatedPayload(
            partnerId,
            ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus.DEACTIVATED,
            2L
        );

        var event = mock(TiBusinessPartnerIdentityDeactivatedEvent.class);
        when(event.getIdentity()).thenReturn(identity);
        when(event.getType()).thenReturn(type);
        when(event.getPayload()).thenReturn(payload);
        return event;
    }
}
