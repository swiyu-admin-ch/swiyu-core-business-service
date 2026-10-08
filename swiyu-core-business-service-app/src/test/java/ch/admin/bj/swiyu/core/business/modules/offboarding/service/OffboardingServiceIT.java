package ch.admin.bj.swiyu.core.business.modules.offboarding.service;

import static ch.admin.bj.swiyu.core.business.test.StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.audit.transactional.outbox.CreateAuditRecordCommandTransactionOutboxSender;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.audit.AuditTrigger;
import ch.admin.bj.swiyu.core.business.common.exceptions.HardDeleteNotAllowedException;
import ch.admin.bj.swiyu.core.business.common.s3.S3ClientAdapter;
import ch.admin.bj.swiyu.core.business.common.s3.S3Properties;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntry;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmission;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.ProtectedVerificationSubmissionTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.VcSchemaSubmissionTestData;
import ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerHardDeletedEvent;
import ch.admin.bj.swiyu.registry.identifier.domain.DidEntityRepository;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntityRepository;
import ch.admin.bj.swiyu.registry.identifier.service.IdentifierRegistryService;
import ch.admin.bj.swiyu.registry.status.domain.StatusListDatastoreEntityRepository;
import ch.admin.bj.swiyu.registry.status.domain.VcEntityRepository;
import ch.admin.bj.swiyu.registry.status.service.StatusListRegistryService;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Full boot on purpose: the point of {@code OffboardingService} is the order in which it touches the
 *  core DB, both registry DBs, the document bucket and PAMS, which a sliced test cannot show. */
@ActiveProfiles("test")
@SpringBootTest
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "test")
class OffboardingServiceIT {

    private static final AuditTrigger TRIGGER = new AuditTrigger.User("bj-user-1", "https://idp.example.com");

    @Autowired
    OffboardingService offboardingService;

    @Autowired
    TestRepositories repos;

    @Autowired
    PartnerDocumentService partnerDocumentService;

    @Autowired
    IdentifierRegistryService identifierRegistryService;

    @Autowired
    StatusListRegistryService statusListRegistryService;

    @Autowired
    IdentifierDatastoreEntityRepository identifierDatastoreEntityRepository;

    @Autowired
    DidEntityRepository didEntityRepository;

    @Autowired
    StatusListDatastoreEntityRepository statusListDatastoreEntityRepository;

    @Autowired
    VcEntityRepository vcEntityRepository;

    @Autowired
    S3ClientAdapter s3ClientAdapter;

    @Autowired
    S3Properties s3Properties;

    @MockitoBean
    PamsClient pamsClient;

    @MockitoBean
    ScanApi scanApi;

    // The senders are mocked, not the publishers above them: keeps the real audit and event building
    // in the test while taking the schema registry out of the picture.
    @MockitoBean
    CreateAuditRecordCommandTransactionOutboxSender auditRecordSender;

    @MockitoBean
    TransactionalOutbox outbox;

    @MockitoSpyBean
    AuditPublisher auditPublisher;

    private UUID partnerId;
    private UUID identifierDatastoreId;
    private UUID statusDatastoreId;
    private UUID documentId;
    private String documentStorageKey;

    @BeforeEach
    void setUp() {
        repos.truncateTables();
        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .result("OK")
                    .requestID(UUID.randomUUID())
                    .description("description")
                    .clamavVersion("clamav-v1")
                    .clamavDatabaseVersion("clamav-db-v1")
            )
        );
    }

    @Test
    void hardDeleteBusinessPartner_removesEverythingInAllThreeDatabasesAndS3() {
        seedPartnerWithDataEverywhere();

        offboardingService.hardDeleteBusinessPartner(partnerId, TRIGGER);

        // core DB
        assertThat(repos.businessPartner.findById(partnerId)).isEmpty();
        assertThat(repos.businessPartnerIdentity.findById(partnerId)).isEmpty();
        assertThat(repos.identifierEntry.findAllByBusinessEntityId(partnerId)).isEmpty();
        assertThat(repos.statusListEntry.findAllByBusinessEntityId(partnerId)).isEmpty();
        assertThat(repos.partnerDocuments.findAllByPartnerId(partnerId)).isEmpty();
        assertThat(repos.trustOnboardingSubmission.findAllByPartnerIdOrderByInitiatedAtAsc(partnerId)).isEmpty();
        assertThat(repos.trustAdditionalDidsSubmission.findAll()).isEmpty();
        assertThat(repos.vcSchemaSubmission.findAll()).isEmpty();
        assertThat(repos.vqpsSubmission.findAll()).isEmpty();
        assertThat(repos.protectedVerificationSubmission.findAll()).isEmpty();

        // identifier registry DB
        assertThat(identifierDatastoreEntityRepository.findById(identifierDatastoreId)).isEmpty();
        assertThat(didEntityRepository.findByBase_Id(identifierDatastoreId)).isEmpty();

        // status registry DB
        assertThat(statusListDatastoreEntityRepository.findById(statusDatastoreId)).isEmpty();
        assertThat(vcEntityRepository.findByBase_Id(statusDatastoreId)).isEmpty();

        // S3 and PAMS
        assertThat(
            s3ClientAdapter.fileExists(
                s3Properties.trustOnboardingSubmissionDocuments().bucketName(),
                documentStorageKey
            )
        ).isFalse();
        verify(pamsClient).deleteBusinessPartner(partnerId.toString());
        assertHardDeletedEventPublished();
    }

    private void assertHardDeletedEventPublished() {
        var message = ArgumentCaptor.forClass(AvroMessage.class);
        verify(outbox).sendMessage(
            message.capture(),
            any(),
            eq(TiBusinessPartnerHardDeletedEvent.TypeRef.DEFAULT_TOPIC)
        );
        assertThat(message.getValue())
            .asInstanceOf(InstanceOfAssertFactories.type(TiBusinessPartnerHardDeletedEvent.class))
            .extracting(event -> event.getPayload().getBusinessPartnerId())
            .isEqualTo(partnerId);
    }

    @Test
    void hardDeleteBusinessPartner_auditsEveryDeletedObjectWithTheTriggerFromTheCommand() {
        seedPartnerWithDataEverywhere();

        offboardingService.hardDeleteBusinessPartner(partnerId, TRIGGER);

        verify(auditPublisher).businessPartnerDeleted(eq(partnerId.toString()), any(), any(), eq(TRIGGER));
        verify(auditPublisher).businessPartnerDocumentDeleted(
            eq(documentId.toString()),
            any(),
            eq(partnerId.toString()),
            any(),
            eq(TRIGGER)
        );
        verify(auditPublisher).identifierEntryDeleted(
            eq(identifierDatastoreId.toString()),
            any(),
            eq(partnerId.toString()),
            any(),
            any(),
            eq(TRIGGER)
        );
        verify(auditPublisher).statusListEntryDeleted(
            eq(statusDatastoreId.toString()),
            any(),
            eq(partnerId.toString()),
            any(),
            any(),
            eq(TRIGGER)
        );
    }

    @Test
    void hardDeleteBusinessPartner_isRepeatableAfterAnInterruptedRun() {
        seedPartnerWithDataEverywhere();

        offboardingService.hardDeleteBusinessPartner(partnerId, TRIGGER);
        // The command is redelivered because the acknowledgement was lost - the second run must be a no-op.
        offboardingService.hardDeleteBusinessPartner(partnerId, TRIGGER);

        assertThat(repos.businessPartner.findById(partnerId)).isEmpty();
        verify(pamsClient, times(1)).deleteBusinessPartner(partnerId.toString());
    }

    @Test
    void hardDeleteBusinessPartner_isRefusedAndChangesNothingWhenTheSafeguardIsNotArmed() {
        seedPartnerWithDataEverywhere();
        var partner = repos.businessPartner.findById(partnerId).orElseThrow();
        partner.unallowHardDelete();
        repos.businessPartner.saveAndFlush(partner);

        assertThatThrownBy(() -> offboardingService.hardDeleteBusinessPartner(partnerId, TRIGGER))
            .isInstanceOf(HardDeleteNotAllowedException.class)
            .hasMessageContaining(partnerId.toString());

        assertThat(repos.businessPartner.findById(partnerId)).isPresent();
        assertThat(repos.identifierEntry.findAllByBusinessEntityId(partnerId)).hasSize(1);
        assertThat(repos.statusListEntry.findAllByBusinessEntityId(partnerId)).hasSize(1);
        assertThat(repos.partnerDocuments.findAllByPartnerId(partnerId)).hasSize(1);
        assertThat(identifierDatastoreEntityRepository.findById(identifierDatastoreId)).isPresent();
        assertThat(statusListDatastoreEntityRepository.findById(statusDatastoreId)).isPresent();
        assertThat(
            s3ClientAdapter.fileExists(
                s3Properties.trustOnboardingSubmissionDocuments().bucketName(),
                documentStorageKey
            )
        ).isTrue();
        verify(pamsClient, never()).deleteBusinessPartner(any());
        verify(auditPublisher, never()).businessPartnerDeleted(any(), any(), any(), any());
    }

    @Test
    void hardDeleteBusinessPartner_isRefusedForAGovernmentalPartner() {
        partnerId = UUID.randomUUID();
        repos.businessPartner.saveAndFlush(BusinessEntityTestData.businessPartnerOfTypeGov(partnerId));

        assertThatThrownBy(() -> offboardingService.hardDeleteBusinessPartner(partnerId, TRIGGER)).isInstanceOf(
            HardDeleteNotAllowedException.class
        );

        assertThat(repos.businessPartner.findById(partnerId)).isPresent();
    }

    @Test
    void hardDeleteBusinessPartner_doesNothingForAnUnknownPartner() {
        var unknownPartnerId = UUID.randomUUID();

        offboardingService.hardDeleteBusinessPartner(unknownPartnerId, TRIGGER);

        verify(pamsClient, never()).deleteBusinessPartner(any());
    }

    /** One row in every table the hard delete has to clear, plus a real object in the document bucket. */
    private void seedPartnerWithDataEverywhere() {
        partnerId = UUID.randomUUID();
        // A BUSINESS partner is armed by default, so nothing has to toggle the safeguard here.
        repos.businessPartner.saveAndFlush(BusinessEntityTestData.businessPartnerOfTypeBusiness(partnerId));
        repos.businessPartnerIdentity.saveAndFlush(BusinessEntityTestData.businessPartnerIdentity(partnerId));

        identifierDatastoreId = identifierRegistryService.createDatastoreEntity().id();
        identifierRegistryService.updateDidTdwEntry(identifierDatastoreId, "[\"did-log-entry\"]");
        repos.identifierEntry.saveAndFlush(new IdentifierEntry(identifierDatastoreId, partnerId));

        statusDatastoreId = statusListRegistryService.createDatastoreEntry().id();
        statusListRegistryService.publishStatusList(statusDatastoreId, VALID_STATUS_LIST_VC_FROM_ISSUER_A);
        repos.statusListEntry.saveAndFlush(new StatusListEntry(statusDatastoreId, partnerId));

        var submission = trustOnboardingSubmission(UUID.randomUUID(), partnerId);
        repos.trustOnboardingSubmission.saveAndFlush(submission);
        repos.trustAdditionalDidsSubmission.saveAndFlush(
            new TrustAdditionalDidsSubmission(
                partnerId,
                new ProofOfPossession("did:example:permission", "nonce-1"),
                List.of(new ProofOfPossession("did:example:new", "nonce-2"))
            )
        );
        repos.vcSchemaSubmission.saveAndFlush(VcSchemaSubmissionTestData.vcSchemaSubmission(partnerId));
        repos.vqpsSubmission.saveAndFlush(VqpsSubmissionTestData.vqpsSubmission(partnerId));
        repos.protectedVerificationSubmission.saveAndFlush(
            ProtectedVerificationSubmissionTestData.protectedVerificationSubmission(partnerId)
        );

        var document = partnerDocumentService.createTrustOnboardingSubmissionDocument(
            partnerId,
            submission.getId(),
            PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER,
            new MockMultipartFile("doc.pdf", "doc.pdf", "application/pdf", "offboarding test".getBytes())
        );
        documentId = document.id();
        documentStorageKey = repos.partnerDocuments.findById(documentId).orElseThrow().getStorageObjectKey();
    }
}
