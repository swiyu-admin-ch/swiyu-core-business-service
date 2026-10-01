package ch.admin.bj.swiyu.core.business.modules.documents.service;

import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "test")
class PartnerDocumentServiceIT {

    @Autowired
    PartnerDocumentService partnerDocumentService;

    @Autowired
    TestRepositories repos;

    @MockitoBean
    ScanApi scanApi;

    @MockitoBean
    ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher auditPublisher;

    @MockitoBean
    ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader didPublicKeyLoader;

    private UUID partnerId;
    private UUID sourceSubmissionId;
    private UUID targetSubmissionId;

    @BeforeEach
    void setUp() {
        repos.truncateTables();

        partnerId = BusinessEntityTestData.ENTITY_A;
        var partner = BusinessEntityTestData.businessPartnerA();
        repos.businessPartner.saveAndFlush(partner);

        var sourceSubmission = trustOnboardingSubmission(
            UUID.randomUUID(),
            partnerId,
            TrustOnboardingSubmissionStatus.UNSUBMITTED
        );
        // Use SUCCEEDED status for source (terminal state) to avoid unique constraint
        sourceSubmission.markAsSucceeded();
        repos.trustOnboardingSubmission.saveAndFlush(sourceSubmission);
        sourceSubmissionId = sourceSubmission.getId();

        var targetSubmission = trustOnboardingSubmission(
            UUID.randomUUID(),
            partnerId,
            TrustOnboardingSubmissionStatus.UNSUBMITTED
        );
        repos.trustOnboardingSubmission.saveAndFlush(targetSubmission);
        targetSubmissionId = targetSubmission.getId();

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
    void copyDocumentsLinksSourceFilesToTargetSubmission() {
        // Create a document on the source submission
        var fileContent = "test document content".getBytes();
        var multipartFile = new MockMultipartFile("doc.pdf", "doc.pdf", "application/pdf", fileContent);

        partnerDocumentService.createTrustOnboardingSubmissionDocument(
            partnerId,
            sourceSubmissionId,
            PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER,
            multipartFile
        );

        // Copy documents from source to target
        partnerDocumentService.copyTrustOnboardingSubmissionDocuments(
            sourceSubmissionId,
            targetSubmissionId,
            partnerId
        );

        // Verify target has the copied document
        var targetDocs = partnerDocumentService.findAllByTrustOnboardingSubmissionId(
            targetSubmissionId,
            org.springframework.data.domain.Pageable.ofSize(10)
        );

        assertThat(targetDocs.getContent()).hasSize(1);
        assertThat(targetDocs.getContent().getFirst().type()).isEqualTo(PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER);
        assertThat(targetDocs.getContent().getFirst().fileName()).isEqualTo("doc.pdf");
        assertThat(targetDocs.getContent().getFirst().trustOnboardingSubmissionId()).isEqualTo(targetSubmissionId);
        assertThat(targetDocs.getContent().getFirst().partnerId()).isEqualTo(partnerId);

        // Original source document should still exist
        var sourceDocs = partnerDocumentService.findAllByTrustOnboardingSubmissionId(
            sourceSubmissionId,
            org.springframework.data.domain.Pageable.ofSize(10)
        );
        assertThat(sourceDocs.getContent()).hasSize(1);
    }

    @Test
    void copyDocumentsWithMultipleTypes() {
        // Create documents of different types on source
        var file1 = new MockMultipartFile("doc1.pdf", "doc1.pdf", "application/pdf", "content1".getBytes());
        var file2 = new MockMultipartFile("doi.pdf", "doi.pdf", "application/pdf", "content2".getBytes());

        partnerDocumentService.createTrustOnboardingSubmissionDocument(
            partnerId,
            sourceSubmissionId,
            PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER,
            file1
        );
        partnerDocumentService.createTrustOnboardingSubmissionDocument(
            partnerId,
            sourceSubmissionId,
            PartnerDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
            file2
        );

        partnerDocumentService.copyTrustOnboardingSubmissionDocuments(
            sourceSubmissionId,
            targetSubmissionId,
            partnerId
        );

        var targetDocs = partnerDocumentService.findAllByTrustOnboardingSubmissionId(
            targetSubmissionId,
            org.springframework.data.domain.Pageable.ofSize(10)
        );

        assertThat(targetDocs.getContent()).hasSize(2);
        var types = targetDocs.getContent().stream().map(PartnerDocumentDto::type).toList();
        assertThat(types).containsExactlyInAnyOrder(
            PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER,
            PartnerDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT
        );
    }

    @Test
    void copyDocumentsWhenSourceHasNoDocumentsDoesNothing() {
        // No documents on source
        partnerDocumentService.copyTrustOnboardingSubmissionDocuments(
            sourceSubmissionId,
            targetSubmissionId,
            partnerId
        );

        var targetDocs = partnerDocumentService.findAllByTrustOnboardingSubmissionId(
            targetSubmissionId,
            org.springframework.data.domain.Pageable.ofSize(10)
        );
        assertThat(targetDocs.getContent()).isEmpty();
    }
}
