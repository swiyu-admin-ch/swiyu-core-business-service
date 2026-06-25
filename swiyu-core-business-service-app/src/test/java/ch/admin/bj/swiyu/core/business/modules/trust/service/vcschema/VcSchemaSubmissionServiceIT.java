package ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.config.JsonSchemaConfig;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.CreateVcMetadataTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustRegistryProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.config.VcTypeMetadataSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmissionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcTypeMetadataValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.VcSchemaSubmissionNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.VcTypeMetadataValidationFailedException;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.VCTypeMetadataTestData;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaSubmissionAcceptedEvent;
import java.net.URI;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@DataJpaTest
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "test")
@Import(
    {
        DataJpaTestConfiguration.class,
        VcSchemaSubmissionService.class,
        VcTypeMetadataValidator.class,
        JsonSchemaConfig.class,
        VcTypeMetadataSchemaConfig.class,
        TrustRegistryProperties.class,
        BusinessPartnerService.class,
        VCTypeMetadataTestData.class,
        DomainEventPublisher.class,
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = "/delete_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = "/insert_test_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_vc_schema_submissions.sql")
@MockitoBean(types = AuditPublisher.class)
class VcSchemaSubmissionServiceIT {

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @MockitoBean
    IdentifierEntryService identifierEntryService;

    @MockitoBean
    StatusListEntryService statusListEntryService;

    @Autowired
    TestRepositories repos;

    @MockitoBean
    TrustRegistryProperties trustRegistryProperties;

    @Autowired
    VcSchemaSubmissionService vcSchemaSubmissionService;

    @Autowired
    VCTypeMetadataTestData testData;

    @Captor
    ArgumentCaptor<TiVcSchemaSubmissionAcceptedEvent> tiVcSchemaSubmissionAcceptedEventArgumentCaptor;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        Mockito.when((trustRegistryProperties.dataServiceBaseUrl())).thenReturn(
            URI.create("https://test-url.ch").toURL()
        );
    }

    @Test
    void createVcSchemaSubmission_shouldCreateSuccessfully() throws Exception {
        doNothing()
            .when(domainEventPublisher)
            .publishVcSchemaSubmissionAcceptedEvent(any(TiVcSchemaSubmissionAcceptedEvent.class));
        var partnerId = BusinessEntityTestData.DEFAULT_ENTITY;
        String fileContent = testData.validTypeMetadata();

        CreateVcMetadataTypeDto dto = new CreateVcMetadataTypeDto(fileContent);

        VcSchemaSubmissionDto result = vcSchemaSubmissionService.createVcSchemaSubmission(dto, partnerId);

        assertNotNull(result);
        assertEquals(partnerId, result.partnerId());
        assertEquals(fileContent, result.file());
        verify(domainEventPublisher).publishVcSchemaSubmissionAcceptedEvent(
            tiVcSchemaSubmissionAcceptedEventArgumentCaptor.capture()
        );
    }

    @Test
    void createVcSchemaSubmission_shouldFailOnInvalidMetadata() {
        UUID partnerId = UUID.fromString(BusinessEntityTestData.DEFAULT_ENTITY_S);
        var invalidFile = "invalid-json";

        CreateVcMetadataTypeDto dto = new CreateVcMetadataTypeDto(invalidFile);

        assertThrows(VcTypeMetadataValidationFailedException.class, () ->
            vcSchemaSubmissionService.createVcSchemaSubmission(dto, partnerId)
        );
    }

    @Test
    void getAllEntities_shouldReturnPagedResults() {
        UUID partnerId = UUID.fromString(BusinessEntityTestData.DEFAULT_ENTITY_S);
        Pageable pageable = PageRequest.of(0, 10);

        Page<VcSchemaSubmissionDto> page = vcSchemaSubmissionService.getAllEntities(partnerId, pageable);

        assertNotNull(page);
        assertTrue(page.getContent().size() >= 0);
    }

    @Test
    void getVcSchemaSubmission_shouldReturnSubmission() {
        UUID existingId = repos.vcSchemaSubmission
            .save(new VcSchemaSubmission(UUID.randomUUID(), testData.validTypeMetadata()))
            .getId();

        VcSchemaSubmissionDto dto = vcSchemaSubmissionService.getVcSchemaSubmission(existingId);

        assertNotNull(dto);
        assertEquals(existingId, dto.id());
    }

    @Test
    void getVcSchemaSubmission_shouldThrowIfNotFound() {
        var nonExistentId = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () ->
            vcSchemaSubmissionService.getVcSchemaSubmission(nonExistentId)
        );
    }

    @Test
    void markAsSucceeded_shouldUpdateStatus() {
        // Arrange
        UUID partnerId = UUID.fromString(BusinessEntityTestData.DEFAULT_ENTITY_S);
        VcSchemaSubmission submission = repos.vcSchemaSubmission.save(
            new VcSchemaSubmission(partnerId, testData.validTypeMetadata())
        );
        UUID id = submission.getId();

        // Act
        vcSchemaSubmissionService.markAsSucceeded(id);

        // Assert
        VcSchemaSubmission updated = repos.vcSchemaSubmission.findById(id).orElseThrow();
        assertSame(VcSchemaSubmissionStatus.SUCCEEDED, updated.getStatus(), "Submission should be marked as succeeded");
    }

    @Test
    void markAsFailed_shouldUpdateStatusAndReason() {
        // Arrange
        UUID partnerId = UUID.fromString(BusinessEntityTestData.DEFAULT_ENTITY_S);
        VcSchemaSubmission submission = repos.vcSchemaSubmission.save(
            new VcSchemaSubmission(partnerId, testData.validTypeMetadata())
        );
        UUID id = submission.getId();
        var failureReason = "Schema validation error";

        // Act
        vcSchemaSubmissionService.markAsFailed(id, failureReason);

        // Assert
        VcSchemaSubmission updated = repos.vcSchemaSubmission.findById(id).orElseThrow();
        assertSame(VcSchemaSubmissionStatus.FAILED, updated.getStatus(), "Submission should be marked as failed");
        assertEquals(failureReason, updated.getFailureReason(), "Failure reason should match");
    }

    @Test
    void markAsSucceeded_shouldThrowIfNotFound() {
        var nonExistentId = UUID.randomUUID();

        assertThrows(VcSchemaSubmissionNotFoundException.class, () ->
            vcSchemaSubmissionService.markAsSucceeded(nonExistentId)
        );
    }

    @Test
    void markAsFailed_shouldThrowIfNotFound() {
        var nonExistentId = UUID.randomUUID();

        assertThrows(VcSchemaSubmissionNotFoundException.class, () ->
            vcSchemaSubmissionService.markAsFailed(nonExistentId, "foobar")
        );
    }
}
