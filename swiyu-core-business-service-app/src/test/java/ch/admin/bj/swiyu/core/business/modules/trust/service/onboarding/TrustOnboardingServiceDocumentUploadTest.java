package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocument.createTrustOnboardingSubissionPartnerDocument;
import static ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentMapper.toPartnerDocumentDto;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmissionRequestDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentType;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentUploadRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionDoiValidationProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.DeclarationOfIntentValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.ProofOfPossessionValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.TrustOnboardingSubmissionDocumentValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.TrustOnboardingSubmissionOnSubmitValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.TrustOnboardingSubmissionValidator;
import ch.admin.suis.client.core.service.IValidationServiceClient;
import ch.admin.suis.validator.rest.to.response.FileReport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;

@ExtendWith(MockitoExtension.class)
class TrustOnboardingServiceDocumentUploadTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;

    @Mock
    private TrustOnboardingSubmissionDomainService trustOnboardingSubmissionDomainService;

    @Mock
    private BusinessPartnerService businessPartnerService;

    @Mock
    private TrustOnboardingSubmissionOnSubmitValidator validator;

    @Mock
    private ProofOfPossessionValidator proofOfPossessionValidator;

    @Mock
    private PartnerDocumentService partnerDocumentService;

    @Mock
    private TrustDeclarationOfIntentPdfService trustDeclarationOfIntentPdfService;

    @Mock
    private TrustOnboardingSubmissionDocumentValidator trustOnboardingSubmissionDocumentValidator;

    @Mock
    private TrustOnboardingSubmissionValidator trustOnboardingSubmissionValidator;

    @Mock
    private TrustOnboardingSubmissionLimitProperties limitProperties;

    @Mock
    private TrustOnboardingSubmissionDoiValidationProperties doiValidationProperties;

    @Mock
    private IValidationServiceClient validationServiceClient;

    @Mock
    private DeclarationOfIntentValidator declarationOfIntentValidator;

    @InjectMocks
    private TrustOnboardingService trustOnboardingService;

    @Test
    void uploadTrustOnboardingSubmissionDocument_updatesDeclarationOfIntent_whenTypeIsDoi() {
        var submission = trustOnboardingSubmission();
        var file = new MockMultipartFile(
            "file",
            "doi.pdf",
            "application/pdf",
            "test-content".getBytes(StandardCharsets.UTF_8)
        );
        var request = TrustOnboardingSubmissionDocumentUploadRequestDto.builder()
            .type(TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT)
            .file(file)
            .build();
        var errors = new BeanPropertyBindingResult(file, "file");
        var fileReport = new FileReport();
        var document = toPartnerDocumentDto(
            createTrustOnboardingSubissionPartnerDocument(
                UUID.randomUUID(),
                submission.getPartnerId(),
                PartnerDocumentType.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                "doi.pdf",
                MediaType.APPLICATION_PDF,
                "storage-key",
                submission.getId(),
                "scan-id",
                Instant.now()
            )
        );

        when(trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(submission.getId())).thenReturn(
            submission
        );
        when(trustOnboardingSubmissionDocumentValidator.validateDocument(submission, file, null)).thenReturn(errors);
        when(declarationOfIntentValidator.validateDeclarationOfIntent(file, submission.getSigningRule())).thenReturn(
            fileReport
        );
        when(
            partnerDocumentService.createTrustOnboardingSubmissionDocument(
                submission.getPartnerId(),
                submission.getId(),
                PartnerDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                file
            )
        ).thenReturn(document);

        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(submission.getId(), request);

        verify(declarationOfIntentValidator).validateDeclarationOfIntent(file, submission.getSigningRule());

        var submissionCaptor = ArgumentCaptor.forClass(TrustOnboardingSubmission.class);
        verify(trustOnboardingSubmissionRepository).saveAndFlush(submissionCaptor.capture());

        assertThat(submissionCaptor.getValue().getDeclarationOfIntent()).isNotNull();
        assertThat(submissionCaptor.getValue().getDeclarationOfIntent().fullySignedDocumentId()).isEqualTo(
            document.id().toString()
        );
        assertThat(submissionCaptor.getValue().getDeclarationOfIntent().validationReport()).isEqualTo(fileReport);
    }

    @Test
    void uploadTrustOnboardingSubmissionDocument_skipsDoiSpecificValidation_whenTypeIsOther() {
        var submission = trustOnboardingSubmission();
        var file = new MockMultipartFile(
            "file",
            "attachment.pdf",
            "application/pdf",
            "test-content".getBytes(StandardCharsets.UTF_8)
        );
        var request = TrustOnboardingSubmissionDocumentUploadRequestDto.builder()
            .type(TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER)
            .file(file)
            .build();
        var errors = new BeanPropertyBindingResult(file, "file");
        var document = toPartnerDocumentDto(
            createTrustOnboardingSubissionPartnerDocument(
                UUID.randomUUID(),
                submission.getPartnerId(),
                PartnerDocumentType.TRUST_ONBOARDING_OTHER,
                "attachment.pdf",
                MediaType.APPLICATION_PDF,
                "storage-key",
                submission.getId(),
                "scan-id",
                Instant.now()
            )
        );

        when(trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(submission.getId())).thenReturn(
            submission
        );
        when(trustOnboardingSubmissionDocumentValidator.validateDocument(submission, file, null)).thenReturn(errors);
        when(
            partnerDocumentService.createTrustOnboardingSubmissionDocument(
                submission.getPartnerId(),
                submission.getId(),
                PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                file
            )
        ).thenReturn(document);

        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(submission.getId(), request);

        verifyNoInteractions(declarationOfIntentValidator);
        verify(trustOnboardingSubmissionRepository, never()).saveAndFlush(any());
        assertThat(submission.getDeclarationOfIntent()).isNull();

        verify(partnerDocumentService).createTrustOnboardingSubmissionDocument(
            submission.getPartnerId(),
            submission.getId(),
            PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER,
            file
        );
    }

    @Test
    void updateTrustOnboardingSubmission_deletesDoiDocument_whenDoiRelevantFieldsChange() {
        var submission = trustOnboardingSubmission();
        var doiDocumentId = UUID.randomUUID();
        submission.updateDeclarationOfIntent(new DeclarationOfIntent(doiDocumentId.toString(), null));

        TrustOnboardingSubmissionRequestDto baseRequest = trustOnboardingSubmissionRequestDto();
        var request = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(baseRequest.getEntityName())
            .entityAddress(baseRequest.entityAddress())
            .entityEmail(baseRequest.entityEmail())
            .contactPerson(baseRequest.getContactPerson())
            .correspondingLanguage(baseRequest.correspondingLanguage())
            .registryIds(Map.of("UID", "CHE-999.999.999"))
            .dids(baseRequest.dids())
            .requestedPartnerType(baseRequest.requestedPartnerType())
            .signingRule(baseRequest.signingRule())
            .signatories(baseRequest.signatories())
            .build();

        when(trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(submission.getId())).thenReturn(
            submission
        );
        when(
            proofOfPossessionValidator.isDidSelectionEqual(submission.getProofOfPossessions(), request.dids())
        ).thenReturn(true);
        when(trustOnboardingSubmissionRepository.saveAndFlush(submission)).thenReturn(submission);

        trustOnboardingService.updateTrustOnboardingSubmission(submission.getId(), request);

        verify(partnerDocumentService).deletePartnerDocument(doiDocumentId);
        assertThat(submission.getDeclarationOfIntent()).isNull();
    }
}
