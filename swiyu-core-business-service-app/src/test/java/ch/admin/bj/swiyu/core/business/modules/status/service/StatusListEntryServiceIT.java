package ch.admin.bj.swiyu.core.business.modules.status.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.DEFAULT_ENTITY;
import static ch.admin.bj.swiyu.core.business.test.IdentifierTestData.identifierEntry_Initialized;
import static ch.admin.bj.swiyu.core.business.test.StatusTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.audit.record.create.CreateAuditRecordCommand;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bj.swiyu.core.business.common.config.JsonSchemaConfig;
import ch.admin.bj.swiyu.core.business.common.did.CryptoIntegrityValidator;
import ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader;
import ch.admin.bj.swiyu.core.business.common.exceptions.ObjectCountLimitApiException;
import ch.admin.bj.swiyu.core.business.modules.identifier.config.IdentifierSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierValidator;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryMapper;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListsLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.status.exceptions.StatusListValidationFailedException;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestKafkaConfiguration;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.registry.status.StatusRegistryConfig;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;

/**
 * Example of an integration test for a service class without bootstrapping the whole application.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(
    {
        DataJpaTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        JsonSchemaConfig.class,
        StatusListEntryService.class,
        StatusRegistryConfig.class,
        StatusListValidator.class,
        StatusListSchemaConfig.class,
        IdentifierSchemaConfig.class,
        IdentifierEntryService.class,
        IdentifierValidator.class,
        IdentifierEntryMapper.class,
        CryptoIntegrityValidator.class,
        DidPublicKeyLoader.class,
    }
)
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
@TestMethodOrder(MethodOrderer.MethodName.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
@WithExtendedJeapAuthenticationToken
class StatusListEntryServiceIT {

    @MockitoSpyBean
    StatusListsLimitProperties statusListsLimitProperties;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    @Qualifier("didResolverClient")
    RestClient didResolverClient;

    @Autowired
    StatusListEntryService statusListEntryService;

    @Autowired
    TestRepositories repos;

    @MockitoBean // registers a callback so we can verify the sent message
    JeapKafkaMessageCallback kafkaMsgCallback;

    @BeforeEach
    void setUp() {
        reset(kafkaMsgCallback);
        when(statusListsLimitProperties.minSize()).thenReturn(DataSize.ofBytes(200));
        when(statusListsLimitProperties.maxSize()).thenReturn(DataSize.ofKilobytes(200));
        when(statusListsLimitProperties.maxAge()).thenReturn(Duration.ofDays(10_000));

        when(
            didResolverClient.get().uri(startsWith(VALID_STATUS_LIST_ISSUER_A_DID_URL)).retrieve().body(String.class)
        ).thenReturn(VALID_STATUS_LIST_ISSUER_A_DID_LOG);
        when(
            didResolverClient.get().uri(startsWith(VALID_STATUS_LIST_ISSUER_B_DID_URL)).retrieve().body(String.class)
        ).thenReturn(VALID_STATUS_LIST_ISSUER_B_DID_LOG);
    }

    @Test
    void createStatusListEntry() {
        // WHEN
        var statusListEntry = statusListEntryService.createStatusListEntry(DEFAULT_ENTITY);
        // THEN
        assertThat(statusListEntry.id()).isNotNull();
        assertThat(statusListEntry.statusRegistryUrl()).isEqualTo(
            "TEST.DATAURL/%s.jwt".formatted(statusListEntry.id())
        );
    }

    @Test
    void createStatusListEntryLimitReached() {
        // GIVEN
        when(statusListsLimitProperties.defaultMaxCount()).thenReturn((long) 2);
        statusListEntryService.createStatusListEntry(DEFAULT_ENTITY);
        statusListEntryService.createStatusListEntry(DEFAULT_ENTITY);
        assertThatException()
            .isThrownBy(() -> statusListEntryService.createStatusListEntry(DEFAULT_ENTITY))
            // THEN
            .withMessage("Object count limit reached.")
            .matches(e -> {
                var ex = Assertions.assertInstanceOf(ObjectCountLimitApiException.class, e);
                Assertions.assertLinesMatch(
                    List.of("Resources belonging to statuslist_entries has a maximum count of 2."),
                    ex.getAdditionalDetails()
                );
                return true;
            });
    }

    @Test
    void updateStatusListEntry() {
        // GIVEN
        var statusListEntryId = statusListEntryService.createStatusListEntry(DEFAULT_ENTITY).id();
        repos.identifierEntry.save(identifierEntry_Initialized(DEFAULT_ENTITY, VALID_STATUS_LIST_ISSUER_A_DID));
        reset(kafkaMsgCallback); // reset after create so only the update audit is counted during verifyAuditCommandWasSent
        // WHEN / THEN
        Assertions.assertDoesNotThrow(() ->
            statusListEntryService.updateStatusListEntry(
                DEFAULT_ENTITY,
                statusListEntryId,
                VALID_STATUS_LIST_VC_FROM_ISSUER_A
            )
        );
        // check audit
        verifyAuditCommandWasSent(statusListEntryId.toString());
    }

    @Test
    void updateStatusListEntry_validation() {
        // GIVEN
        var statusListEntryId = statusListEntryService.createStatusListEntry(DEFAULT_ENTITY).id();

        // WHEN Wrongly serialized jwt is sent
        var exception = Assertions.assertThrows(StatusListValidationFailedException.class, () ->
            statusListEntryService.updateStatusListEntry(DEFAULT_ENTITY, statusListEntryId, null)
        );

        // THEN Throw general exception
        assertThat(exception.getMessage()).isEqualTo("Provided status list resource is invalid.");
        assertThat(exception.getAdditionalDetails().getFirst()).isEqualTo("Status list VC is null or empty");
    }

    @Test
    void getStatusListEntryList() {
        // GIVEN
        statusListEntryService.createStatusListEntry(DEFAULT_ENTITY);
        statusListEntryService.createStatusListEntry(DEFAULT_ENTITY);
        statusListEntryService.createStatusListEntry(DEFAULT_ENTITY);
        var pagination = PageRequest.of(1, 2);
        // WHEN
        var result = statusListEntryService.getPagedByBusinessPartner(DEFAULT_ENTITY, pagination);
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
    }

    private void verifyAuditCommandWasSent(String auditObjectId) {
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getPayload().getAuditedData().getId()).isEqualTo(auditObjectId);
    }
}
