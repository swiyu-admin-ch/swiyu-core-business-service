package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerTypeDto;
import static ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocument.createTrustOnboardingSubissionPartnerDocument;
import static ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentMapper.toPartnerDocumentDto;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustOnboardingMapper.*;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.util.CollectionUtils.isEmpty;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.exceptions.ValidationException;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentType;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidationResult;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentUploadRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.DeclarationOfIntentValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.ProofOfPossessionValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.TrustOnboardingSubmissionDocumentValidator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import tools.jackson.databind.node.JsonNodeFactory;

@ExtendWith(MockitoExtension.class)
class TrustOnboardingServiceDocumentUploadTest {

    @Mock
    private TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;

    @Mock
    private TrustOnboardingSubmissionDomainService trustOnboardingSubmissionDomainService;

    @Mock
    private ProofOfPossessionValidator proofOfPossessionValidator;

    @Mock
    private PartnerDocumentService partnerDocumentService;

    @Mock
    private DeclarationOfIntentValidator declarationOfIntentValidator;

    @Mock
    private TrustOnboardingSubmissionDocumentValidator trustOnboardingSubmissionDocumentValidator;

    @InjectMocks
    private TrustOnboardingService trustOnboardingService;

    @BeforeEach
    void setUp() {
        // The service calls the void 2-arg validator overload, so default it to no-op.
        lenient().doNothing().when(trustOnboardingSubmissionDocumentValidator).validateCanUpload(any(), any());
    }

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
        var validationResult = declarationOfIntentValidationResult();

        when(trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(submission.getId())).thenReturn(
            submission
        );
        doNothing().when(trustOnboardingSubmissionDocumentValidator).validateCanUpload(submission, file);
        when(declarationOfIntentValidator.validateDeclarationOfIntent(file, submission.getSigningRule())).thenReturn(
            declarationOfIntentValidationResult()
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
        assertThat(submissionCaptor.getValue().getDeclarationOfIntent().getFullySignedDocumentId()).isEqualTo(
            document.id()
        );
        assertThat(submissionCaptor.getValue().getDeclarationOfIntent().getValidationReport()).isEqualTo(
            validationResult.fileReport()
        );
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
        doNothing().when(trustOnboardingSubmissionDocumentValidator).validateCanUpload(submission, file);
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
        submission.updateDeclarationOfIntent(new DeclarationOfIntent(doiDocumentId, null));

        TrustOnboardingSubmissionRequestDto baseRequest = trustOnboardingSubmissionRequestDto();
        var request = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(baseRequest.getEntityName())
            .entityAddress(baseRequest.entityAddress())
            .entityEmail(baseRequest.entityEmail())
            .contactPerson(baseRequest.getContactPerson())
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

    @Test
    void updateTrustOnboardingSubmission_keepsDoiDocument_whenOnlyAddressChangesForRenewal() {
        // GIVEN
        var submission = trustOnboardingSubmissionOfTypeRenewalWithDoi();
        var doiDocumentId = submission.getDeclarationOfIntent().getFullySignedDocumentId();
        var request = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(submission.getEntityName())
            .entityAddress(
                AddressDto.builder() // changed address
                    .street("New Street")
                    .postalCode("9999")
                    .city("New City")
                    .country("CH")
                    .build()
            )
            .entityEmail(submission.getEntityEmail())
            .contactPerson(toContactDto(submission.getContactPerson()))
            .registryIds(toRegistryIds(submission.getUid()))
            .dids(toDids(submission.getProofOfPossessions()))
            .requestedPartnerType(toBusinessPartnerTypeDto(submission.getRequestedPartnerType()))
            .signingRule(toSigningRuleDto(submission.getSigningRule()))
            .signatories(toSignatoryDtos(submission.getSignatories()))
            .build();

        when(trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(submission.getId())).thenReturn(
            submission
        );
        when(
            proofOfPossessionValidator.isDidSelectionEqual(submission.getProofOfPossessions(), request.dids())
        ).thenReturn(true);
        when(trustOnboardingSubmissionRepository.saveAndFlush(submission)).thenReturn(submission);

        // WHEN
        trustOnboardingService.updateTrustOnboardingSubmission(submission.getId(), request);

        verify(partnerDocumentService, never()).deletePartnerDocument(any());
        assertThat(submission.getDeclarationOfIntent()).isNotNull();
        assertThat(submission.getDeclarationOfIntent().getFullySignedDocumentId()).isEqualTo(doiDocumentId);
    }

    @Test
    void uploadDocumentFailsForRenewalSubmission() {
        var submission = trustOnboardingSubmission(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustOnboardingSubmissionType.RENEWAL
        );
        var file = new MockMultipartFile("doc.pdf", "doc.pdf", "application/pdf", "x".getBytes());
        var errors = new BeanPropertyBindingResult(file, "file");
        errors.reject("editing_blocked", "Cannot add documents to RENEWAL submission");

        when(trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(submission.getId())).thenReturn(
            submission
        );
        doThrow(new ValidationException("TrustOnboardingSubmissionDocument has missing/invalid fields.", errors))
            .when(trustOnboardingSubmissionDocumentValidator)
            .validateCanUpload(submission, file);

        var request = TrustOnboardingSubmissionDocumentUploadRequestDto.builder()
            .type(TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER)
            .file(file)
            .build();

        var submissionId = submission.getId();
        assertThatThrownBy(() -> trustOnboardingService.uploadTrustOnboardingSubmissionDocument(submissionId, request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("editing_blocked");
    }

    private DeclarationOfIntentValidationResult declarationOfIntentValidationResult() {
        return new DeclarationOfIntentValidationResult(JsonNodeFactory.instance.objectNode());
    }

    private List<String> toDids(List<ProofOfPossession> proofOfPossessions) {
        if (isEmpty(proofOfPossessions)) {
            return emptyList();
        }
        return proofOfPossessions.stream().map(ProofOfPossession::getDid).toList();
    }
}
