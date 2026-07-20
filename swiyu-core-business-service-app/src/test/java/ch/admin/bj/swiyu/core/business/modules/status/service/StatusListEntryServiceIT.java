package ch.admin.bj.swiyu.core.business.modules.status.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerDefault;
import static ch.admin.bj.swiyu.core.business.test.IdentifierTestData.identifierEntry_Initialized;
import static ch.admin.bj.swiyu.core.business.test.StatusTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListsLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.status.exceptions.StatusListValidationFailedException;
import ch.admin.bj.swiyu.core.business.test.*;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
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
@WithExtendedJeapAuthenticationToken
class StatusListEntryServiceIT {

    @MockitoBean
    BusinessPartnerService businessPartnerService;

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

    // A few tests should skip certain validations
    @MockitoSpyBean
    StatusListValidator statusListValidator;

    @BeforeEach
    void setUp() {
        repos.truncateTables();

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
        // GIVEN
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        // WHEN
        var statusListEntry = statusListEntryService.createStatusListEntry(partnerId);
        // THEN
        assertThat(statusListEntry.id()).isNotNull();
        assertThat(statusListEntry.statusRegistryUrl()).isEqualTo(
            "TEST.DATAURL/%s.jwt".formatted(statusListEntry.id())
        );
    }

    @Test
    void createStatusListEntryLimitReached() {
        // GIVEN
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        when(statusListsLimitProperties.defaultMaxCount()).thenReturn((long) 2);
        statusListEntryService.createStatusListEntry(partnerId);
        statusListEntryService.createStatusListEntry(partnerId);
        assertThatException()
            .isThrownBy(() -> statusListEntryService.createStatusListEntry(partnerId))
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
    void getLimits_returnsCorrectValues() {
        // GIVEN
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        when(statusListsLimitProperties.defaultMaxCount()).thenReturn(5L);
        var limitsEmpty = statusListEntryService.getLimits(partnerId);
        assertThat(limitsEmpty.count().current()).isZero();
        assertThat(limitsEmpty.count().max()).isEqualTo(5L);
        // WHEN create an entry
        statusListEntryService.createStatusListEntry(partnerId);
        // THEN limits reflect count 1
        var limitsAfter = statusListEntryService.getLimits(partnerId);
        assertThat(limitsAfter.count().current()).isEqualTo(1);
        assertThat(limitsAfter.count().max()).isEqualTo(5L);
    }

    @Test
    void updateStatusListEntry() {
        // GIVEN
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        var statusListEntryId = statusListEntryService.createStatusListEntry(partnerId).id();
        repos.identifierEntry.save(identifierEntry_Initialized(partnerId, VALID_STATUS_LIST_ISSUER_A_DID));
        reset(kafkaMsgCallback); // reset after create so only the update audit is counted during verifyAuditCommandWasSent
        // WHEN / THEN
        Assertions.assertDoesNotThrow(() ->
            statusListEntryService.updateStatusListEntry(
                partnerId,
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
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        var statusListEntryId = statusListEntryService.createStatusListEntry(partnerId).id();

        // WHEN Wrongly serialized jwt is sent
        var exception = Assertions.assertThrows(StatusListValidationFailedException.class, () ->
            statusListEntryService.updateStatusListEntry(partnerId, statusListEntryId, null)
        );

        // THEN Throw general exception
        assertThat(exception.getMessage()).isEqualTo("Provided status list resource is invalid.");
        assertThat(exception.getAdditionalDetails().getFirst()).isEqualTo("Status list VC is null or empty");
    }

    @Test
    void getStatusListEntryList() {
        // GIVEN
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        statusListEntryService.createStatusListEntry(partnerId);
        statusListEntryService.createStatusListEntry(partnerId);
        statusListEntryService.createStatusListEntry(partnerId);
        var pagination = PageRequest.of(1, 2);
        // WHEN
        var result = statusListEntryService.getPagedByBusinessPartner(partnerId, pagination);
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

    @Test
    void statusListValid() {
        // GIVEN
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        repos.identifierEntry.save(identifierEntry_Initialized(partnerId, VALID_STATUS_LIST_ISSUER_A_DID));
        var statusListEntryId = statusListEntryService.createStatusListEntry(partnerId).id();
        var statusListEntry = repos.statusListEntry.findById(statusListEntryId).orElseThrow();

        // WHEN / THEN no error
        Assertions.assertDoesNotThrow(() ->
            statusListValidator.validateStatusListVc(statusListEntry, VALID_STATUS_LIST_VC_FROM_ISSUER_A)
        );
    }

    @Test
    void statusListInvalid_throwsWithStatusListNotFromSameIssuer() {
        // GIVEN
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        var statusListEntryId = statusListEntryService.createStatusListEntry(partnerId).id();
        repos.identifierEntry.save(identifierEntry_Initialized(partnerId, VALID_STATUS_LIST_ISSUER_B_DID));
        statusListEntryService.updateStatusListEntry(partnerId, statusListEntryId, VALID_STATUS_LIST_VC_FROM_ISSUER_B);
        var entry = repos.statusListEntry.findById(statusListEntryId).orElseThrow();

        // WHEN / THEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(entry, StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
        );

        // THEN Throw general exception
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains(
            "Statuslist VC issuer does not match the issuer of the already uploaded statuslist."
        );
    }

    @Test
    void swissProfile_v1_0_0_valid() {
        // GIVEN
        var partnerId = repos.businessPartner.save(businessPartnerDefault()).getId();
        repos.identifierEntry.save(identifierEntry_Initialized(partnerId, VALID_STATUS_LIST_ISSUER_A_DID));
        var statusListEntryId = statusListEntryService.createStatusListEntry(partnerId).id();
        var entry = repos.statusListEntry.findById(statusListEntryId).orElseThrow();
        doNothing().when(statusListValidator).checkStatusListCryptoIntegrity(any());

        // WHEN / THEN no error
        Assertions.assertDoesNotThrow(() ->
            statusListValidator.validateStatusListVcV2(entry, VALID_SWISS_PROFILE_STATUS_LIST_VC)
        );
    }
}
