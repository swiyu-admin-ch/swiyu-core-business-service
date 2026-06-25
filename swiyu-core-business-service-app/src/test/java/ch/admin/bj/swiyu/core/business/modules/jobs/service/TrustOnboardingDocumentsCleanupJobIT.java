package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.exceptions.DocumentNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentListItemDto;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentUploadRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionDomainService;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.nio.charset.StandardCharsets;
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
@WithJeapAuthenticationToken(username = "Test")
@WithAllTestContainerInitializers
@MockitoBean(types = AuditPublisher.class)
class TrustOnboardingDocumentsCleanupJobIT {

    @Autowired
    private TestRepositories testRepositories;

    @Autowired
    private TrustOnboardingSubmissionDomainService trustOnboardingSubmissionDomainService;

    @Autowired
    private TrustOnboardingService trustOnboardingService;

    @Autowired
    private PartnerDocumentService partnerDocumentService;

    @Autowired
    private TrustOnboardingDocumentsCleanupJob job;

    @MockitoBean
    ScanApi scanApi;

    @BeforeEach
    void setUp() {
        testRepositories.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(testRepositories.businessPartner);
    }

    @Test
    void testRemovalOfExpiredDocuments() {
        // GIVEN
        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .clamavDatabaseVersion("test")
                    .clamavVersion("test")
                    .description("test")
                    .filename("test")
                    .result("OK")
                    .requestID(UUID.randomUUID())
            )
        );
        var tosA = trustOnboardingSubmission(UUID.randomUUID(), BusinessEntityTestData.ENTITY_A);
        testRepositories.trustOnboardingSubmission.save(tosA);
        var docId = uploadTestDocument(tosA).id();
        // we have to reload entity as it was edited in the createFor function
        tosA = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(tosA.getId());
        tosA.markAsExpired();
        testRepositories.trustOnboardingSubmission.save(tosA);

        // WHEN
        job.triggerCleanupTrustOnboardingSubmissionDocuments();

        // THEN
        var tosAId = tosA.getId();
        assertThatThrownBy(() ->
            partnerDocumentService.getDocumentForTrustOnboardingSubmission(tosAId, docId)
        ).isInstanceOf(DocumentNotFoundException.class);
    }

    private TrustOnboardingSubmissionDocumentListItemDto uploadTestDocument(TrustOnboardingSubmission tos) {
        MockMultipartFile file = new MockMultipartFile(
            "test.doc",
            "test.doc",
            "application/pdf",
            "A".getBytes(StandardCharsets.UTF_8)
        );
        return trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            tos.getId(),
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                file
            )
        );
    }
}
