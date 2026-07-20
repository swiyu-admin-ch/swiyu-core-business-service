package ch.admin.bj.swiyu.core.business.modules.identifier.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.DEFAULT_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.IdentifierUpdateRequestDto;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.config.JsonSchemaConfig;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import ch.admin.bj.swiyu.core.business.common.exceptions.ObjectCountLimitApiException;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryFilterDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.config.IdentifierLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.identifier.config.IdentifierSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierValidator;
import ch.admin.bj.swiyu.core.business.modules.identifier.exceptions.IdentifierValidationFailedException;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.IdentifierTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryConfig;
import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.unit.DataSize;

@DataJpaTest
@ActiveProfiles("test")
@Import(
    {
        DataJpaTestConfiguration.class,
        IdentifierRegistryConfig.class,
        IdentifierTestData.class,
        IdentifierEntryService.class,
        IdentifierValidator.class,
        JsonSchemaConfig.class,
        IdentifierSchemaConfig.class,
        IdentifierEntryMapper.class,
    }
)
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
class IdentityEntryServiceIT {

    @Autowired
    IdentifierEntryService identifierEntryService;

    @MockitoBean
    AuditPublisher auditPublisher;

    @MockitoSpyBean
    IdentifierLimitProperties identifierLimitProperties;

    @MockitoSpyBean
    IdentifierRegistryProperties identifierRegistryProperties;

    @Autowired
    TestRepositories repos;

    @Autowired
    IdentifierTestData identifierTestData;

    @BeforeEach
    void setUp() {
        repos.identifierEntry.deleteAllInBatch();
        doReturn(new IdentifierLimitProperties.DidDocLimits(DataSize.ofMegabytes(10)))
            .when(identifierLimitProperties)
            .didDoc();
        doReturn(new IdentifierLimitProperties.DidLogLimits(DataSize.ofMegabytes(10)))
            .when(identifierLimitProperties)
            .didLog();
    }

    @Test
    void createIdentifierEntry() {
        // GIVEN
        var identifierRegistryEntryUrl = "https://test.identifier.registry/";
        when(identifierRegistryProperties.defaultPublicResolveUrlTemplate()).thenReturn(
            identifierRegistryEntryUrl + "%s"
        );
        // WHEN
        var identifierEntry = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY);
        // THEN
        assertThat(identifierEntry.identifierRegistryUrl()).isEqualTo(
            identifierRegistryEntryUrl + identifierEntry.id()
        );
    }

    @Test
    void createIdentifierEntryLimitReached() {
        // GIVEN
        when(identifierLimitProperties.defaultMaxCount()).thenReturn((long) 2);
        identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY);
        identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY);
        // WHEN / THEN
        var ex = assertThrows(ObjectCountLimitApiException.class, () ->
            identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY)
        );
        assertThat(ex.getMessage()).isEqualTo("Object count limit reached.");
    }

    @Test
    void getLimits_returnsCorrectValues() {
        // GIVEN
        when(identifierLimitProperties.defaultMaxCount()).thenReturn(5L);
        // Ensure repository is empty (setup does deleteAll)
        var limitsEmpty = identifierEntryService.getLimits(DEFAULT_ENTITY);
        assertThat(limitsEmpty.count().current()).isZero();
        assertThat(limitsEmpty.count().max()).isEqualTo(5L);
        // WHEN create an entry
        identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY);
        // THEN limits reflect count 1
        var limitsAfter = identifierEntryService.getLimits(DEFAULT_ENTITY);
        assertThat(limitsAfter.count().current()).isEqualTo(1);
        assertThat(limitsAfter.count().max()).isEqualTo(5L);
    }

    @Test
    void updateIdentifierEntry() {
        // GIVEN
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        var entryBefore = repos.identifierEntry.findById(identifierEntryId).orElseThrow();
        repos.commit();
        // WHEN // THEN
        assertDoesNotThrow(() ->
            identifierEntryService.updateIdentifierEntry(
                DEFAULT_ENTITY,
                identifierEntryId,
                identifierTestData.validDidTdwLog()
            )
        );
        var entryAfter = repos.identifierEntry.findById(identifierEntryId).orElseThrow();
        assertThat(entryAfter.getUploadCount()).isEqualTo(entryBefore.getUploadCount() + 1);
    }

    @Test
    void updateIdentifierEntryV1_2_0() {
        // GIVEN
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        var uploadCountBefore = repos.identifierEntry.findById(identifierEntryId).orElseThrow().getUploadCount();
        // WHEN // THEN
        assertDoesNotThrow(() ->
            identifierEntryService.updateIdentifierEntry(
                DEFAULT_ENTITY,
                identifierEntryId,
                identifierTestData.validDidTdwLogV1_2_0()
            )
        );
        var entryAfter = repos.identifierEntry.findById(identifierEntryId).orElseThrow();
        assertThat(entryAfter.getUploadCount()).isEqualTo(uploadCountBefore + 1);
        assertThat(entryAfter.getDid()).isEqualTo(
            "did:tdw:QmNPSsZ3DosVyQ1WrwFTpRSPYsqq9YZDLDqizdPSWCJzaS:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:e0e1f8ec-cbba-47dd-abd3-008a111af18a"
        );
    }

    @Test
    void updateIdentifierEntryV2_0_0() {
        // GIVEN
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        var uploadCountBefore = repos.identifierEntry.findById(identifierEntryId).orElseThrow().getUploadCount();
        // WHEN // THEN
        assertDoesNotThrow(() ->
            identifierEntryService.updateIdentifierEntry(
                DEFAULT_ENTITY,
                identifierEntryId,
                identifierTestData.validDidWebvhLogV2_0_0()
            )
        );
        var entryAfter = repos.identifierEntry.findById(identifierEntryId).orElseThrow();
        assertThat(entryAfter.getUploadCount()).isEqualTo(uploadCountBefore + 1);
        assertThat(entryAfter.getDid()).isEqualTo(
            "did:webvh:QmUMHj7fLuU2tgrDVC4awCqafsDXA7G4KBw1QpYniKKzA2:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:45d801da-a8f5-4479-8996-a02216d790dc"
        );
    }

    @Test
    void updateIdentifierEntryV2_1_0() {
        // GIVEN
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        var uploadCountBefore = repos.identifierEntry.findById(identifierEntryId).orElseThrow().getUploadCount();
        // WHEN // THEN
        assertDoesNotThrow(() ->
            identifierEntryService.updateIdentifierEntry(
                DEFAULT_ENTITY,
                identifierEntryId,
                identifierTestData.validDidWebvhLogV2_1_0()
            )
        );
        var entryAfter = repos.identifierEntry.findById(identifierEntryId).orElseThrow();
        assertThat(entryAfter.getUploadCount()).isEqualTo(uploadCountBefore + 1);
        assertThat(entryAfter.getDid()).isEqualTo(
            "did:webvh:QmTUVaiyhzG1ZQJmZq1YYj4eTxRqsjBXqZmhq4kwFSD5eJ:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:7d7700e5-357c-4a7b-989b-86eeecb12293"
        );
    }

    @Test
    void updateIdentifierEntry_failure_when_did_fork() {
        // GIVEN
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        var initialPayload = identifierTestData.validDidWebvhUpdatedEntry();
        identifierEntryService.updateIdentifierEntry(DEFAULT_ENTITY, identifierEntryId, initialPayload);

        var updatePayload = identifierTestData.validDidWebvhEntry();

        // WHEN // THEN
        var ex = assertThrows(IdentifierValidationFailedException.class, () ->
            identifierEntryService.updateIdentifierEntry(DEFAULT_ENTITY, identifierEntryId, updatePayload)
        );
        assertThat(ex.getErrorCode()).isEqualTo(BusinessExceptionErrorCode.IDENTIFIER_VALIDATION_FAILED);
    }

    @Test
    void updateIdentifierEntry_userDeactivated() {
        // GIVEN
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        var didTdwLog = identifierTestData.validDidTdwLog();
        identifierEntryService.updateIdentifierEntry(DEFAULT_ENTITY, identifierEntryId, didTdwLog);

        // status = user_deactivated -> upload must not be possible
        var identifierEntry = repos.identifierEntry.findById(identifierEntryId).orElseThrow();
        identifierEntry.updateDidAndDeactivate(identifierEntry.getDid());
        repos.identifierEntry.saveAndFlush(identifierEntry);
        repos.commit();

        // WHEN // THEN
        assertThrows(IdentifierValidationFailedException.class, () ->
            identifierEntryService.updateIdentifierEntry(DEFAULT_ENTITY, identifierEntryId, didTdwLog)
        );

        var entryAfter = repos.identifierEntry.findById(identifierEntryId).orElseThrow();
        // upload count still same as before
        assertThat(entryAfter.getUploadCount()).isEqualTo(identifierEntry.getUploadCount());
    }

    @Test
    void getIdentifierEntryList() {
        // GIVEN
        identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY);
        identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY);
        identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY);
        var pagination = PageRequest.of(1, 2);
        // WHEN
        var result = identifierEntryService.searchIdentifierEntries(
            IdentifierEntryFilterDto.builder().businessPartnerId(DEFAULT_ENTITY).build(),
            pagination
        );
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateIdentifierEntryDescription() {
        // GIVEN
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        var newDescription = "Updated description";

        // WHEN
        identifierEntryService.updateIdentifierEntryDescription(
            DEFAULT_ENTITY,
            identifierEntryId,
            new IdentifierUpdateRequestDto(newDescription)
        );

        // THEN
        var entryAfter = identifierEntryService.getIdentifierEntry(DEFAULT_ENTITY, identifierEntryId);
        assertThat(entryAfter.description()).isEqualTo(newDescription);
    }
}
