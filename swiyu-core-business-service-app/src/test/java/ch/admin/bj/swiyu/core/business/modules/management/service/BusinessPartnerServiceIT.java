package ch.admin.bj.swiyu.core.business.modules.management.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeGov;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeUnknown;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreateBusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreatePartnerDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.UpdateBusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestKafkaConfiguration;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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

    @Autowired
    TestRepositories repos;

    @Autowired
    BusinessPartnerService businessPartnerService;

    private static String lookupPamsAdminUserUid() {
        return (
            (JeapAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()
        ).getPreferredUsername();
    }

    @Test
    void getBusinessEntities() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeUnknown(UUID.randomUUID()));
        repos.commit();
        // WHEN
        var readEntity = businessPartnerService.getBusinessEntity(partner.getId());
        // THEN
        assertThat(partner.getId()).isNotNull();
        assertThat(readEntity).isPresent();
        assertThat(readEntity.get().name()).isEqualTo(LocalizedMapUtil.getDefaultValue(partner.getEntityName()));
        assertThat(readEntity.get().id()).isEqualTo(partner.getId());
    }

    @Test
    void getBusinessPartners() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeUnknown(UUID.randomUUID()));
        repos.commit();
        // WHEN
        var readEntity = businessPartnerService.getBusinessPartner(partner.getId());
        // THEN
        assertThat(partner.getId()).isNotNull();
        assertThat(readEntity.name()).isEqualTo(LocalizedMapUtil.getDefaultValue(partner.getEntityName()));
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
    void createBusinessPartnerV1() {
        // GIVEN
        var createBusinessEntityDto = new CreateBusinessEntityDto("Hallo Welt AG", "hello.world@example.com", null);
        // WHEN
        var businessEntity = businessPartnerService.createBusinessPartnerV1(
            createBusinessEntityDto,
            lookupPamsAdminUserUid()
        );
        // THEN
        assertThat(businessEntity).isNotNull();
        assertThat(businessEntity.id()).isNotNull();
    }

    @Test
    void createGovernmentalBusinessEntity() {
        // GIVEN
        var createBusinessEntityDto = new CreateBusinessEntityDto(
            "Hallo Welt AG",
            "hello.world@example.com",
            BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION
        );
        // WHEN
        var businessEntity = businessPartnerService.createBusinessPartnerV1(
            createBusinessEntityDto,
            lookupPamsAdminUserUid()
        );
        // THEN
        assertThat(businessEntity).isNotNull();
        assertThat(businessEntity.id()).isNotNull();
    }

    @Test
    void createBusinessPartnerV2() {
        // GIVEN
        var createBusinessEntityDto = new CreatePartnerDto(
            "Hallo Welt AG",
            null,
            "uid",
            "addressStreet",
            "addressZipCode",
            "addressCity",
            "addressCountry",
            "addressRegion",
            "contactPhone",
            "hello.world@example.com"
        ); // WHEN
        var businessEntity = businessPartnerService.createBusinessPartnerV2(
            createBusinessEntityDto,
            lookupPamsAdminUserUid()
        );
        // THEN
        assertThat(businessEntity).isNotNull();
        assertThat(businessEntity.id()).isNotNull();
    }

    @Test
    void createGovernmentalBusinessPartner() {
        // GIVEN
        var createBusinessEntityDto = new CreatePartnerDto(
            "Hallo Welt AG",
            BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION,
            "uid",
            "addressStreet",
            "addressZipCode",
            "addressCity",
            "addressCountry",
            "addressRegion",
            "contactPhone",
            "hello.world@example.com"
        ); // WHEN
        var businessEntity = businessPartnerService.createBusinessPartnerV2(
            createBusinessEntityDto,
            lookupPamsAdminUserUid()
        );
        // THEN
        assertThat(businessEntity).isNotNull();
        assertThat(businessEntity.id()).isNotNull();
    }

    @Test
    void updateBusinessEntity() {
        // GIVEN
        var createBusinessEntityDto = new CreateBusinessEntityDto("Hallo Welt AG", "hello.world@example.com", null);
        var oldBusinessEntity = businessPartnerService.createBusinessPartnerV1(
            createBusinessEntityDto,
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
    }

    @Test
    void updateBusinessPartner() {
        // GIVEN
        var businessEntity = businessPartnerOfTypeUnknown(UUID.randomUUID());
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
    void updateBusinessEntityGovStatus_withNonExistingActor_throws() {
        // GIVEN
        var inexistantId = UUID.fromString("deadbeef-0000-0000-0000-000000000000");
        // WHEN / THEN
        Assertions.assertThatThrownBy(() ->
            businessPartnerService.updateBusinessEntityIsGovernment(inexistantId, true)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateBusinessEntityGovStatus_withExistingGovActor() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        repos.commit();

        // WHEN / THEN
        Assertions.assertThat(businessPartnerService.updateBusinessEntityIsGovernment(partner.getId(), true)).isNull();
    }

    @Test
    void updateBusinessEntityGovStatus_withExistingNonGovActorToTrue() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeUnknown(UUID.randomUUID()));
        repos.commit();

        // WHEN / THEN
        var updatedPartner = businessPartnerService.updateBusinessEntityIsGovernment(partner.getId(), true);
        Assertions.assertThat(updatedPartner).isNotNull();
        Assertions.assertThat(updatedPartner.id()).isEqualTo(partner.getId());
    }

    @Test
    void updateBusinessEntityGovStatus_withExistingNonGovActorToFalse() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeUnknown(UUID.randomUUID()));
        repos.commit();

        // WHEN / THEN
        var updatedPartner = businessPartnerService.updateBusinessEntityIsGovernment(partner.getId(), false);
        Assertions.assertThat(updatedPartner).isNull();
    }

    @Test
    void deleteBusinessEntity_withExistingPartner() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeUnknown(UUID.randomUUID()));
        repos.commit();

        // WHEN / THEN
        Assertions.assertThat(businessPartnerService.getBusinessEntity(partner.getId())).isPresent();
        businessPartnerService.deleteBusinessEntity(partner.getId());
        Assertions.assertThat(businessPartnerService.getBusinessEntity(partner.getId())).isNotPresent();
    }

    @Test
    void validateIsGovernmental_withNull_shouldPass() {
        // GIVEN / WHEN / THEN
        assertFalse(() -> businessPartnerService.isGovernmental(null));
    }

    @Test
    void validateIsGovernmental_withGovernmentalEntity_shouldPass() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        repos.commit();

        // WHEN & THEN
        assertDoesNotThrow(() -> businessPartnerService.isGovernmental(partner.getId()));
        assertTrue(() -> businessPartnerService.isGovernmental(partner.getId()));
    }

    @Test
    void validateIsGovernmental_withNonGovernmentalEntity_shouldThrow() {
        // GIVEN
        var partner = repos.businessPartner.save(businessPartnerOfTypeUnknown(UUID.randomUUID()));
        repos.commit();

        // WHEN
        UUID nonGovId = partner.getId();

        // THEN
        assertFalse(() -> businessPartnerService.isGovernmental(nonGovId));
    }

    @Test
    void validateIsGovernmental_withNonExistentEntity_shouldThrow() {
        // GIVEN
        var randomId = UUID.randomUUID();

        // WHEN & THEN
        assertFalse(() -> businessPartnerService.isGovernmental(randomId));
    }
}
