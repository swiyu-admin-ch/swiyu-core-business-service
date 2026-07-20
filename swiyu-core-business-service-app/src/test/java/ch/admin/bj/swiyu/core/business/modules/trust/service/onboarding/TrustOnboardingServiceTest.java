package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionDomainService;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrustOnboardingServiceTest {

    private TrustOnboardingSubmissionRepository mockRepository;
    private TrustOnboardingService trustOnboardingService;

    @BeforeEach
    void setUp() {
        mockRepository = mock(TrustOnboardingSubmissionRepository.class);

        trustOnboardingService = new TrustOnboardingService(
            mock(DomainEventPublisher.class),
            mockRepository,
            mock(TrustOnboardingSubmissionDomainService.class),
            mock(BusinessPartnerService.class),
            mock(TrustOnboardingSubmissionOnSubmitValidator.class),
            mock(ProofOfPossessionValidator.class),
            mock(PartnerDocumentService.class),
            mock(TrustDeclarationOfIntentPdfService.class),
            mock(TrustOnboardingSubmissionDocumentValidator.class),
            mock(TrustOnboardingSubmissionValidator.class),
            mock(TrustOnboardingSubmissionLimitProperties.class),
            mock(DeclarationOfIntentValidator.class),
            mock(AuditPublisher.class)
        );
    }

    @Test
    void createTrustOnboardingSubmission_alreadyExisting_returnExisting1() {
        var submissionId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        var status = TrustOnboardingSubmissionStatus.UNSUBMITTED;
        var initiatedAt = Instant.now().minus(1, ChronoUnit.SECONDS);
        var dbEntry = trustOnboardingSubmission(submissionId, partnerId, status, initiatedAt);
        when(
            mockRepository.findByPartnerIdAndStatusIn(
                partnerId,
                List.of(
                    TrustOnboardingSubmissionStatus.SUBMITTED,
                    TrustOnboardingSubmissionStatus.UNSUBMITTED,
                    TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
                )
            )
        ).thenReturn(dbEntry);

        var request = trustOnboardingSubmissionRequestDtoWithOnlyPartnerId(partnerId);
        var response = trustOnboardingService.createTrustOnboardingSubmission(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(submissionId);
        assertThat(response.partnerId()).isEqualTo(partnerId);
        assertThat(response.name()).isEqualTo(dbEntry.getEntityName());
        assertThat(response.entityEmail()).isEqualTo(dbEntry.getEntityEmail());
        assertThat(response.address()).isNotNull();
        assertThat(response.address().street()).isEqualTo(dbEntry.getEntityAddress().getStreet());
        assertThat(response.address().city()).isEqualTo(dbEntry.getEntityAddress().getCity());
        assertThat(response.address().postalCode()).isEqualTo(dbEntry.getEntityAddress().getPostalCode());
        assertThat(response.address().country()).isEqualTo(dbEntry.getEntityAddress().getCountry());
        assertThat(response.address().region()).isEqualTo(dbEntry.getEntityAddress().getRegion());
        assertThat(response.contactPerson()).isNotNull();
        assertThat(response.contactPerson().firstName()).isEqualTo(dbEntry.getContactPerson().getFirstName());
        assertThat(response.contactPerson().lastName()).isEqualTo(dbEntry.getContactPerson().getLastName());
        assertThat(response.contactPerson().email()).isEqualTo(dbEntry.getContactPerson().getEmail());
        assertThat(response.contactPerson().phone()).isEqualTo(dbEntry.getContactPerson().getPhone());
        assertThat(response.status()).hasToString(status.toString());
        assertThat(response.proofOfPossessions()).hasSize(2);
        assertThat(response.businessPartnerType()).hasToString(dbEntry.getRequestedPartnerType().toString());
    }

    @Test
    void createTrustOnboardingSubmission_alreadyExisting_returnExisting2() {
        var submissionId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        var status = TrustOnboardingSubmissionStatus.UNSUBMITTED;
        var initiatedAt = Instant.now().minus(1, ChronoUnit.SECONDS);
        var dbEntry = trustOnboardingSubmission(submissionId, partnerId, status, initiatedAt);
        when(
            mockRepository.findByPartnerIdAndStatusIn(
                partnerId,
                List.of(
                    TrustOnboardingSubmissionStatus.SUBMITTED,
                    TrustOnboardingSubmissionStatus.UNSUBMITTED,
                    TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
                )
            )
        ).thenReturn(dbEntry);

        var request = trustOnboardingSubmissionRequestDtoWithOnlyPartnerId(partnerId);
        var response = trustOnboardingService.createTrustOnboardingSubmission(request);

        assertThat(response.signingRule()).hasToString(dbEntry.getSigningRule().toString());
        assertThat(response.signatories()).hasSameSizeAs(dbEntry.getSignatories());
        assertThat(response.signatories().getFirst().firstName()).isEqualTo(
            dbEntry.getSignatories().getFirst().firstName()
        );
        assertThat(response.signatories().getFirst().lastName()).isEqualTo(
            dbEntry.getSignatories().getFirst().lastName()
        );
        assertThat(response.signatories().getFirst().phone()).isEqualTo(dbEntry.getSignatories().getFirst().phone());
        assertThat(response.signatories().getFirst().email()).isEqualTo(dbEntry.getSignatories().getFirst().email());
        assertThat(response.registryIds()).containsEntry("UID", "CHE-123.456.789");
        assertThat(response.isRegisteredInCommercialRegister()).isTrue();
        assertThat(response.rejectionReason()).isNull();
        assertThat(response.declineReason()).isNull();
        assertThat(response.partnerNote()).isNull();
        assertThat(response.correspondingLanguage()).hasToString(dbEntry.getCorrespondingLanguage().toString());
        assertThat(response.initiatedAt()).isCloseTo(initiatedAt, within(10, ChronoUnit.MILLIS));
        assertThat(response.createdAt()).isNull();
        assertThat(response.submittedAt()).isNull();
        assertThat(response.updatedAt()).isNull();

        verify(mockRepository, never()).save(any());
    }

    @Test
    void createTrustOnboardingSubmissionWithRegistryId_hasResponseUIDSet() {
        when(mockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = trustOnboardingSubmissionRequestDto();
        var response = trustOnboardingService.createTrustOnboardingSubmission(request);

        assertThat(response).isNotNull();
        assertThat(response.partnerId()).isEqualTo(request.partnerId());
        assertThat(response.name()).isEqualTo(request.entityName());
        assertThat(response.entityEmail()).isEqualTo(request.entityEmail());
        assertThat(response.address()).isEqualTo(request.getEntityAddress());
        assertThat(response.contactPerson()).isEqualTo(request.getContactPerson());
        assertThat(response.status()).hasToString(TrustOnboardingSubmissionStatus.UNSUBMITTED.toString());
        assertThat(response.proofOfPossessions()).hasSize(request.dids().size());
        assertThat(response.businessPartnerType()).isEqualTo(request.requestedPartnerType());
        assertThat(response.signingRule()).isEqualTo(request.signingRule());
        assertThat(response.signatories()).isEqualTo(request.signatories());
        assertThat(response.registryIds()).isEqualTo(request.registryIds());
        assertThat(response.isRegisteredInCommercialRegister()).isTrue();
        assertThat(response.rejectionReason()).isNull();
        assertThat(response.declineReason()).isNull();
        assertThat(response.partnerNote()).isNull();
        assertThat(response.correspondingLanguage()).isEqualTo(request.correspondingLanguage());
        assertThat(response.initiatedAt()).isCloseTo(Instant.now(), within(500, ChronoUnit.MILLIS));
        assertThat(response.createdAt()).isNull();
        assertThat(response.submittedAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void createTrustOnboardingSubmissionWithoutRegistryId_hasResponseUIDUnset() {
        when(mockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = trustOnboardingSubmissionRequestDtoWithoutUID();
        var response = trustOnboardingService.createTrustOnboardingSubmission(request);

        assertThat(response).isNotNull();
        assertThat(response.partnerId()).isEqualTo(request.partnerId());
        assertThat(response.name()).isEqualTo(request.entityName());
        assertThat(response.entityEmail()).isEqualTo(request.entityEmail());
        assertThat(response.address()).isEqualTo(request.getEntityAddress());
        assertThat(response.contactPerson()).isEqualTo(request.getContactPerson());
        assertThat(response.status()).hasToString(TrustOnboardingSubmissionStatus.UNSUBMITTED.toString());
        assertThat(response.proofOfPossessions()).hasSize(request.dids().size());
        assertThat(response.businessPartnerType()).isEqualTo(request.requestedPartnerType());
        assertThat(response.signingRule()).isEqualTo(request.signingRule());
        assertThat(response.signatories()).isEqualTo(request.signatories());
        assertThat(response.registryIds()).isEmpty();
        assertThat(response.isRegisteredInCommercialRegister()).isTrue();
        assertThat(response.rejectionReason()).isNull();
        assertThat(response.declineReason()).isNull();
        assertThat(response.partnerNote()).isNull();
        assertThat(response.correspondingLanguage()).isEqualTo(request.correspondingLanguage());
        assertThat(response.initiatedAt()).isCloseTo(Instant.now(), within(500, ChronoUnit.MILLIS));
        assertThat(response.createdAt()).isNull();
        assertThat(response.submittedAt()).isNull();
        assertThat(response.updatedAt()).isNull();
    }
}
