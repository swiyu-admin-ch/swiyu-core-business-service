package ch.admin.bj.swiyu.core.business.modules.management.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeBusiness;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeGov;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessDataIntegrityViolationException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreatePartnerDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.UpdateBusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestKafkaConfiguration;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityUpdatedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerUpdatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

/**
 * Example of an integration test for a service class without bootstrapping the whole application.
 */
@ActiveProfiles("test")
@DataJpaTest
@WithJeapAuthenticationToken(username = "test")
@WithAllTestContainerInitializers
@Import({ DataJpaTestConfiguration.class, DataJpaTestKafkaConfiguration.class, BusinessPartnerService.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
class BusinessPartnerServiceIT {

    @MockitoBean
    IdentifierEntryService identifierEntryService;

    @MockitoBean
    StatusListEntryService statusListEntryService;

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @MockitoBean
    AuditPublisher auditPublisher;

    @Autowired
    TestRepositories repos;

    @Autowired
    BusinessPartnerService businessPartnerService;

    @Test
    void getBusinessEntities() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID()));
        repos.commit();
        // WHEN
        var readEntity = businessPartnerService.getBusinessEntity(partner.getId());
        // THEN
        assertThat(partner.getId()).isNotNull();
        assertThat(readEntity).isPresent();
        assertThat(readEntity.get().name()).isEqualTo(LocalizedMapUtil.getDefaultValue(partner.getEntityName()));
        assertThat(readEntity.get().id()).isEqualTo(partner.getId());
    }

    @SuppressWarnings("java:S1874") // remove with EID-6624
    @Test
    void getBusinessPartners() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID()));
        repos.commit();
        // WHEN
        var readEntity = businessPartnerService.getBusinessPartner(partner.getId());
        // THEN
        assertThat(partner.getId()).isNotNull();
        assertThat(readEntity.entityName()).isEqualTo(partner.getEntityName());
        assertThat(readEntity.id()).isEqualTo(partner.getId());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @Test
    void getBusinessEntity_db_inserted() {
        var readEntity = businessPartnerService.getBusinessEntity(
            UUID.fromString("deadbeef-deaf-0000-0000-000000000000")
        );
        // THEN
        assertThat(readEntity).isPresent();
    }

    @Test
    void createBusinessPartnerV2() {
        // GIVEN
        var createBusinessEntityDto = new CreatePartnerDto(
            "Hallo Welt AG",
            BusinessPartnerTypeDto.BUSINESS,
            "uid",
            BusinessEntityTestData.someAddressDto(),
            BusinessEntityTestData.someContactDto()
        );
        // WHEN
        var businessEntity = businessPartnerService.createBusinessPartnerV2(
            createBusinessEntityDto,
            lookupPamsAdminUserUid()
        );
        // THEN
        assertThat(businessEntity).isNotNull();
        assertThat(businessEntity.id()).isNotNull();
        // non-governmental partners start armed for hard delete
        assertThat(businessEntity.hardDeleteAllowed()).isTrue();
        verify(domainEventPublisher).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> event.getPayload().getBusinessPartnerId().equals(businessEntity.id()))
        );
    }

    @Test
    void createGovernmentalBusinessPartnerV2() {
        // GIVEN
        var createBusinessEntityDto = new CreatePartnerDto(
            "Hallo Welt AG",
            BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION,
            "uid",
            BusinessEntityTestData.someAddressDto(),
            BusinessEntityTestData.someContactDto()
        ); // WHEN
        var businessEntity = businessPartnerService.createBusinessPartnerV2(
            createBusinessEntityDto,
            lookupPamsAdminUserUid()
        );
        // THEN
        assertThat(businessEntity).isNotNull();
        assertThat(businessEntity.id()).isNotNull();
        // governmental institutions are always locked against hard delete
        assertThat(businessEntity.hardDeleteAllowed()).isFalse();
    }

    @Test
    void updateBusinessEntity() {
        // GIVEN
        var oldBusinessEntity = businessPartnerService.createBusinessPartnerV2(
            BusinessEntityTestData.createPartnerDto(),
            lookupPamsAdminUserUid()
        );
        var updateBusinessEntityDto = new UpdateBusinessEntityDto("example name", "hello.brave.new.world@example.com");
        // WHEN
        var businessEntity = businessPartnerService.updateBusinessEntity(
            oldBusinessEntity.id(),
            updateBusinessEntityDto
        );
        // THEN
        assertThat(businessEntity).isNotNull();
        assertThat(businessEntity.contactEmailAddress()).isEqualTo("hello.brave.new.world@example.com");
        assertThat(businessEntity.id()).isEqualTo(oldBusinessEntity.id());
        assertThat(businessEntity.name()).isEqualTo("example name");

        var updatedPartner = businessPartnerService.getBusinessPartner(oldBusinessEntity.id());
        assertThat(LocalizedMapUtil.getDefaultValue(updatedPartner.entityName())).isEqualTo("example name");

        var updatedEntity = repos.businessPartner.findById(oldBusinessEntity.id()).orElseThrow();
        assertThat(LocalizedMapUtil.getDefaultValue(updatedEntity.getEntityName())).isEqualTo("example name");

        // one change notification for the create, one for the update
        verify(domainEventPublisher, times(2)).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> event.getPayload().getBusinessPartnerId().equals(oldBusinessEntity.id()))
        );
    }

    @SuppressWarnings("java:S1874") // remove with EID-6624
    @Test
    void updateBusinessPartner() {
        // GIVEN
        var businessEntity = businessPartnerOfTypeBusiness(UUID.randomUUID());
        repos.businessPartner.save(businessEntity);
        repos.commit();

        var newName = "New Name";
        var newAddress = new Address("New Street", "New City", "1234", "CH", "Region");
        var newEmail = "new@example.com";
        var newUid = "CHE-123.456.789";
        var newPhone = "+41 79 123 45 67";
        var newType = BusinessPartnerType.BUSINESS;

        // WHEN
        businessPartnerService.updateBusinessPartner(
            businessEntity.getId(),
            LocalizedMapUtil.fromSingleName(newName),
            newAddress,
            newEmail,
            newUid,
            newPhone,
            newType
        );

        // THEN
        var updatedEntity = repos.businessPartner.findById(businessEntity.getId()).orElseThrow();
        assertThat(LocalizedMapUtil.getDefaultValue(updatedEntity.getEntityName())).isEqualTo(newName);
        assertThat(updatedEntity.getContactEmail()).isEqualTo(newEmail);
        assertThat(updatedEntity.getUid()).isEqualTo(newUid);
        assertThat(updatedEntity.getContactPhone()).isEqualTo(newPhone);
        assertThat(updatedEntity.getAddress().getStreet()).isEqualTo(newAddress.getStreet());
        assertThat(updatedEntity.getType()).isEqualTo(newType);
        verify(domainEventPublisher).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> event.getPayload().getBusinessPartnerId().equals(businessEntity.getId()))
        );
    }

    @Test
    void updateBusinessEntity_thenNotFound() {
        // GIVEN
        var inexistantId = UUID.fromString("deadbeef-0000-0000-0000-000000000000");
        var businessEntityDto = new UpdateBusinessEntityDto("example name", "hello.brave.new.world@example.com");
        // WHEN / THEN
        Assertions.assertThatThrownBy(() ->
            businessPartnerService.updateBusinessEntity(inexistantId, businessEntityDto)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteBusinessEntity_withExistingPartner() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID()));
        repos.commit();

        // WHEN / THEN
        Assertions.assertThat(businessPartnerService.getBusinessEntity(partner.getId())).isPresent();
        businessPartnerService.deleteBusinessEntity(partner.getId());
        Assertions.assertThat(businessPartnerService.getBusinessEntity(partner.getId())).isNotPresent();
    }

    @Test
    void isGovernmental_withNull_shouldThrow() {
        // GIVEN / WHEN / THEN
        assertThatThrownBy(() -> businessPartnerService.isGovernmental(null)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void isGovernmental_withGovernmentalEntity_shouldBeTrue() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        repos.commit();

        // WHEN & THEN
        assertDoesNotThrow(() -> businessPartnerService.isGovernmental(partner.getId()));
        assertTrue(() -> businessPartnerService.isGovernmental(partner.getId()));
    }

    @Test
    void isGovernmental_withNonGovernmentalEntity_shouldBeFalse() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID()));
        repos.commit();

        // WHEN
        UUID nonGovId = partner.getId();

        // THEN
        assertFalse(() -> businessPartnerService.isGovernmental(nonGovId));
    }

    @Test
    void isGovernmental_withNonExistentEntity_shouldThrow() {
        // GIVEN
        var randomId = UUID.randomUUID();

        // WHEN & THEN
        assertThatThrownBy(() -> businessPartnerService.isGovernmental(randomId)).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    /**
     * The service reports whether there was an identity to deactivate; the caller uses that to decide
     * whether the partner has to be notified. The email itself is published by
     * {@code TiBusinessPartnerIdentityEventProcessor} - see its test.
     */
    @Test
    void deactivateBusinessPartnerIdentity_reportsThatAnIdentityWasDeactivated() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        partner.updateBusinessPartnerIdentity(BusinessEntityTestData.businessPartnerIdentity());
        repos.businessPartner.save(partner);
        repos.commit();

        // WHEN
        var deactivated = businessPartnerService.deactivateBusinessPartnerIdentity(partner.getId(), 2L);

        // THEN
        assertThat(deactivated).isTrue();
    }

    @Test
    void deactivateBusinessPartnerIdentity_withoutIdentity_reportsThatNothingWasDeactivated() {
        // GIVEN - a partner that never received a BPI from TMS
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        repos.commit();

        // WHEN
        var deactivated = businessPartnerService.deactivateBusinessPartnerIdentity(partner.getId(), 2L);

        // THEN
        assertThat(deactivated).isFalse();
    }

    @Test
    void publishBusinessPartnerUpdatedEvent_publishesTheChangeNotification() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID()));
        repos.commit();

        // WHEN - twice: idempotent, may be called repeatedly
        businessPartnerService.publishBusinessPartnerUpdatedEvent(partner.getId());
        businessPartnerService.publishBusinessPartnerUpdatedEvent(partner.getId());

        // THEN - two notifications, no state change (no version bump)
        verify(domainEventPublisher, times(2)).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> event.getPayload().getBusinessPartnerId().equals(partner.getId()))
        );
        var reloaded = repos.businessPartner.findById(partner.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(partner.getVersion());
    }

    @Test
    void publishBusinessPartnerUpdatedEvent_unknownPartner_throwsAndPublishesNothing() {
        // GIVEN / WHEN / THEN
        var unknownId = UUID.randomUUID();
        assertThatThrownBy(() -> businessPartnerService.publishBusinessPartnerUpdatedEvent(unknownId)).isInstanceOf(
            ResourceNotFoundException.class
        );
        verify(domainEventPublisher, never()).publishTiBusinessPartnerUpdatedEvent(
            org.mockito.ArgumentMatchers.any(TiBusinessPartnerUpdatedEvent.class)
        );
    }

    @Test
    void publishAllBusinessPartnerUpdatedEvents_publishesOneEventPerPartner() {
        // GIVEN
        var partnerA = repos.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID()));
        var partnerB = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        repos.commit();

        // WHEN
        businessPartnerService.publishAllBusinessPartnerUpdatedEvents();

        // THEN
        verify(domainEventPublisher).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> event.getPayload().getBusinessPartnerId().equals(partnerA.getId()))
        );
        verify(domainEventPublisher).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> event.getPayload().getBusinessPartnerId().equals(partnerB.getId()))
        );
    }

    /**
     * BPI changes come from TMS via the TiBusinessPartnerIdentity* events - publishing the sync
     * event for them would echo them back (TMS -> CBS -> TMS).
     */
    @Test
    void applyingOrDeactivatingBusinessPartnerIdentity_publishesNoBusinessPartnerUpdatedEvent() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        repos.commit();

        // WHEN
        businessPartnerService.applyUpdatedBusinessPartnerIdentity(
            partner.getId(),
            businessPartnerIdentityUpdatedPayload()
        );
        businessPartnerService.deactivateBusinessPartnerIdentity(partner.getId(), 2L);

        // THEN
        verify(domainEventPublisher, never()).publishTiBusinessPartnerUpdatedEvent(
            org.mockito.ArgumentMatchers.any(TiBusinessPartnerUpdatedEvent.class)
        );
    }

    @Test
    void changeHardDeleteAllowed_locksAndArmsThePartner_auditsAndPublishes() {
        // GIVEN - a non-governmental partner, armed by default
        var partner = repos.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID()));
        repos.commit();
        assertThat(partner.isHardDeleteAllowed()).isTrue();

        // WHEN - lock, then arm again
        var locked = businessPartnerService.changeHardDeleteAllowed(partner.getId(), false);
        var armed = businessPartnerService.changeHardDeleteAllowed(partner.getId(), true);

        // THEN - flag toggled, every change audited and published
        assertThat(locked.hardDeleteAllowed()).isFalse();
        assertThat(armed.hardDeleteAllowed()).isTrue();
        assertThat(repos.businessPartner.findById(partner.getId()).orElseThrow().isHardDeleteAllowed()).isTrue();
        verify(auditPublisher, times(2)).businessPartnerUpdated(
            org.mockito.ArgumentMatchers.eq(partner.getId().toString()),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
        verify(domainEventPublisher, times(2)).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> event.getPayload().getBusinessPartnerId().equals(partner.getId()))
        );
    }

    @Test
    void changeHardDeleteAllowed_armingAGovernmentalPartner_isRefused() {
        // GIVEN - governmental partners are always locked
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        repos.commit();
        assertThat(partner.isHardDeleteAllowed()).isFalse();

        // WHEN / THEN
        var partnerId = partner.getId();
        assertThatThrownBy(() -> businessPartnerService.changeHardDeleteAllowed(partnerId, true)).isInstanceOf(
            BusinessDataIntegrityViolationException.class
        );
        assertThat(repos.businessPartner.findById(partner.getId()).orElseThrow().isHardDeleteAllowed()).isFalse();
        verify(domainEventPublisher, never()).publishTiBusinessPartnerUpdatedEvent(
            org.mockito.ArgumentMatchers.any(TiBusinessPartnerUpdatedEvent.class)
        );
    }

    @Test
    void changeHardDeleteAllowed_unknownPartner_throwsNotFound() {
        var unknownId = UUID.randomUUID();
        assertThatThrownBy(() -> businessPartnerService.changeHardDeleteAllowed(unknownId, false)).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void updateBusinessPartner_changingTypeToGovernmental_locksHardDelete() {
        // GIVEN - an armed BUSINESS partner
        var businessEntity = repos.businessPartner.save(businessPartnerOfTypeBusiness(UUID.randomUUID()));
        repos.commit();
        assertThat(businessEntity.isHardDeleteAllowed()).isTrue();

        // WHEN - the type changes to GOVERNMENTAL_INSTITUTION
        businessPartnerService.updateBusinessPartner(
            businessEntity.getId(),
            LocalizedMapUtil.fromSingleName("Gov Now"),
            businessEntity.getAddress(),
            "gov@example.com",
            businessEntity.getUid(),
            null,
            BusinessPartnerType.GOVERNMENTAL_INSTITUTION
        );

        // THEN - the safeguard locks automatically
        assertThat(
            repos.businessPartner.findById(businessEntity.getId()).orElseThrow().isHardDeleteAllowed()
        ).isFalse();
    }

    private static String lookupPamsAdminUserUid() {
        return (
            (JeapAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()
        ).getPreferredUsername();
    }

    private static BusinessPartnerIdentityUpdatedPayload businessPartnerIdentityUpdatedPayload() {
        return new BusinessPartnerIdentityUpdatedPayload(
            UUID.randomUUID(),
            Instant.now(),
            List.of("did:example:partner1", "did:example:partner2"),
            BusinessPartnerIdentityStatus.ACTIVE,
            Instant.now(),
            "CHE-123.456.789",
            Map.of("default", "Test Partner AG"),
            1L
        );
    }
}
