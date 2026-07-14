package ch.admin.bj.swiyu.core.business.modules.dataimport.service;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.CoreDemoData;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.MockMultipartFile;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentsRepository;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntryRepository;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentUploadRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import ch.admin.bj.swiyu.registry.identifier.domain.DatastoreStatus;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntity;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({ "java:S1192", "java:S5803", "java:S1854" })
@Component
@Profile("test-data-injection")
@RequiredArgsConstructor
@Slf4j
public class DemoDataImportService {

    private final BusinessPartnerRepository businessEntityRepository;
    private final IdentifierEntryRepository identifierEntryRepository;
    private final IdentifierDatastoreEntityRepository identifierDatastoreEntityRepository;
    private final PartnerDocumentsRepository partnerDocumentsRepository;
    private final TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;
    private final TrustOnboardingService trustOnboardingService;
    private final PartnerDocumentService partnerDocumentService;

    private final IdentifierEntryService identifierEntryService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> generateBusinessPartners() {
        log.debug("Importing demo business partners...");
        var data = List.of(
            new BusinessEntity(
                CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY,
                LocalizedMapUtil.getDefaultValue(CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY_NAMES),
                CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY_EMAIL,
                BusinessPartnerType.INDIVIDUAL,
                CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY_ADDRESS,
                null,
                CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY_PHONE
            ),
            new BusinessEntity(
                CoreDemoData.CORE_ID_BP_DEFAULT,
                LocalizedMapUtil.getDefaultValue(CoreDemoData.CORE_ID_BP_DEFAULT_NAMES),
                CoreDemoData.CORE_ID_BP_DEFAULT_EMAIL,
                BusinessPartnerType.INDIVIDUAL,
                null,
                null,
                CoreDemoData.CORE_ID_BP_DEFAULT_PHONE
            ),
            new BusinessEntity(
                CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED,
                LocalizedMapUtil.getDefaultValue(CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_NAMES),
                CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_EMAIL,
                BusinessPartnerType.INDIVIDUAL,
                null,
                null,
                null
            ),
            new BusinessEntity(
                CoreDemoData.CORE_ID_BP_GOV,
                LocalizedMapUtil.getDefaultValue(CoreDemoData.CORE_ID_BP_GOV_NAMES),
                CoreDemoData.CORE_ID_BP_GOV_EMAIL,
                BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
                null,
                null,
                null
            ),
            new BusinessEntity(
                CoreDemoData.CORE_ID_BP_E2ETESTS,
                LocalizedMapUtil.getDefaultValue(CoreDemoData.CORE_ID_BP_E2ETESTS_NAMES),
                CoreDemoData.CORE_ID_BP_E2ETESTS_EMAIL,
                BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
                null,
                null,
                null
            )
        );
        for (var d : data) {
            var optDbEntity = businessEntityRepository.findById(d.getId());
            if (optDbEntity.isPresent()) {
                var dbEntity = optDbEntity.get();
                dbEntity.overwriteFrom(d);
                businessEntityRepository.saveAndFlush(dbEntity);
            } else {
                businessEntityRepository.saveAndFlush(d);
            }
        }
        return data.stream().map(BusinessEntity::getId).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateIdentifierEntries(List<UUID> partnerIds) {
        log.debug("Importing demo identifier entries ...");
        for (UUID partnerId : partnerIds) {
            var optEntity = identifierEntryRepository.findById(partnerId);
            if (optEntity.isEmpty()) {
                var entity = new IdentifierEntry(
                    partnerId, // same
                    partnerId
                );
                entity.updateDidAndActivate("did:example:" + partnerId);
                identifierEntryRepository.saveAndFlush(entity);
            }
        }

        var identifierDatastoreEntityOpt = identifierDatastoreEntityRepository.findById(
            CoreDemoData.CORE_ID_IDENTIFIER_E2ETESTS_LOCAL
        );
        var identifierDatastoreEntity = identifierDatastoreEntityOpt.orElseGet(() ->
            identifierDatastoreEntityRepository.save(
                new IdentifierDatastoreEntity(CoreDemoData.CORE_ID_IDENTIFIER_E2ETESTS_LOCAL)
            )
        );
        identifierDatastoreEntity.changeStatus(DatastoreStatus.ACTIVE);
        identifierDatastoreEntityRepository.save(identifierDatastoreEntity);

        var identifierEntryOpt = identifierEntryRepository.findById(CoreDemoData.CORE_ID_IDENTIFIER_E2ETESTS_LOCAL);
        var identifierEntry = identifierEntryOpt.orElseGet(() ->
            identifierEntryRepository.save(
                new IdentifierEntry(CoreDemoData.CORE_ID_IDENTIFIER_E2ETESTS_LOCAL, CoreDemoData.CORE_ID_BP_E2ETESTS)
            )
        );
        identifierEntry.setDescription("DID for local E2E tests");
        identifierEntryRepository.save(identifierEntry);
        identifierEntryService.updateIdentifierEntry(
            CoreDemoData.CORE_ID_BP_E2ETESTS,
            CoreDemoData.CORE_ID_IDENTIFIER_E2ETESTS_LOCAL,
            CoreDemoData.CORE_ID_IDENTIFIER_E2ETESTS_LOCAL_DIDLOG
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteDemoTrustOnboardingSubmissions() {
        deleteAllDocumentsByPartner(CoreDemoData.CORE_ID_BP_DEFAULT);
        deleteAllDocumentsByPartner(CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED);
        deleteAllDocumentsByPartner(CoreDemoData.CORE_ID_BP_GOV);
        deleteAllDocumentsByPartner(CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY);

        trustOnboardingSubmissionRepository.deleteByPartnerId(CoreDemoData.CORE_ID_BP_DEFAULT);
        trustOnboardingSubmissionRepository.deleteByPartnerId(CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED);
        trustOnboardingSubmissionRepository.deleteByPartnerId(CoreDemoData.CORE_ID_BP_GOV);
        trustOnboardingSubmissionRepository.deleteByPartnerId(CoreDemoData.CORE_ID_BP_BASE_ONBOARDING_ONLY);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateTrustOnboardingSubmissions() {
        TrustOnboardingSubmission sub;

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.CORE_ID_TOS_SUCCEEDED,
            CoreDemoData.CORE_ID_BP_DEFAULT,
            CoreDemoData.CORE_ID_BP_DEFAULT_NAMES,
            CoreDemoData.CORE_ID_BP_DEFAULT_ADDRESS,
            CoreDemoData.CORE_ID_BP_DEFAULT_CONTACT,
            CoreDemoData.CORE_ID_BP_DEFAULT_EMAIL,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            CoreDemoData.CORE_ID_BP_DEFAULT_SIGNATORIES
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.CORE_ID_TOS_SUCCEEDED,
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent.pdf", "something new")
            )
        );
        sub.markAsSucceeded();
        trustOnboardingService.aggregateTrustVerificationStatus(sub.getPartnerId());
        trustOnboardingSubmissionRepository.saveAndFlush(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.CORE_ID_TOS_INFO_REQUESTED,
            CoreDemoData.CORE_ID_BP_GOV,
            CoreDemoData.CORE_ID_BP_GOV_NAMES,
            CoreDemoData.CORE_ID_BP_GOV_ADDRESS,
            CoreDemoData.CORE_ID_BP_GOV_CONTACT,
            CoreDemoData.CORE_ID_BP_GOV_EMAIL,
            BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
            SigningRule.JOINT_SIGNATURE_THREE,
            CoreDemoData.CORE_ID_BP_GOV_SIGNATORIES
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.CORE_ID_TOS_INFO_REQUESTED,
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent.pdf", "something different")
            )
        );
        sub.markAsInformationRequested(TrustOnboardingDeclineReason.MISSING_DOCUMENTS, "Test note data");
        trustOnboardingService.aggregateTrustVerificationStatus(sub.getPartnerId());
        trustOnboardingSubmissionRepository.saveAndFlush(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.CORE_ID_TOS_REJECTED,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_NAMES,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_ADDRESS,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_CONTACT,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_EMAIL,
            BusinessPartnerType.INDIVIDUAL,
            null,
            List.of()
        );
        sub.markAsRejected(TrustOnboardingRejectReason.FRAUDULENT_ACTIVITY);
        trustOnboardingService.aggregateTrustVerificationStatus(sub.getPartnerId());
        trustOnboardingSubmissionRepository.saveAndFlush(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.CORE_ID_TOS_UNSUBMITTED,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_NAMES,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_ADDRESS,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_CONTACT,
            CoreDemoData.CORE_ID_BP_WANTS_TO_BE_TRUSTED_EMAIL,
            BusinessPartnerType.INDIVIDUAL,
            null,
            List.of()
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.CORE_ID_TOS_UNSUBMITTED,
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent from .pdf", "something")
            )
        );
        trustOnboardingService.aggregateTrustVerificationStatus(sub.getPartnerId());

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.CORE_ID_TOS_SUBMITTED,
            CoreDemoData.CORE_ID_BP_DEFAULT,
            CoreDemoData.CORE_ID_BP_DEFAULT_NAMES,
            CoreDemoData.CORE_ID_BP_DEFAULT_ADDRESS,
            CoreDemoData.CORE_ID_BP_DEFAULT_CONTACT,
            CoreDemoData.CORE_ID_BP_DEFAULT_EMAIL,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            CoreDemoData.CORE_ID_BP_DEFAULT_SIGNATORIES
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.CORE_ID_TOS_SUBMITTED,
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent.pdf", "something else")
            )
        );
        for (var filename : List.of(
            "Handelsregister.pdf",
            "Vertrag_Kaufvertrag_Musterfirma_AG_2025.pdf",
            "Dienstleistungsvertrag_ProjektX_KundeABC_07-11-2025.pdf",
            "Rahmenvertrag_LieferantXYZ_Version2.0.pdf",
            "Mietvertrag_Bürofläche_Zürich_Unterschrieben.pdf",
            "Kooperationsvertrag_Partnerfirma_Gültig_ab_01-01-2026.pdf",
            "Arbeitsvertrag_Max_Muster_Unterschrift_2025.pdf",
            "Geheimhaltungsvereinbarung_NDA_KundeGHI_ProjektY.pdf"
        )) {
            trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
                CoreDemoData.CORE_ID_TOS_SUBMITTED,
                new TrustOnboardingSubmissionDocumentUploadRequestDto(
                    TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                    new MockMultipartFile(filename, "something with UID")
                )
            );
        }

        sub.markAsSubmitted();
        trustOnboardingService.aggregateTrustVerificationStatus(sub.getPartnerId());
        trustOnboardingSubmissionRepository.save(sub);
    }

    private TrustOnboardingSubmission generateTrustOnboardingSubmission( // NOSONAR
        UUID tosId,
        UUID partnerId,
        Map<String, String> entityName,
        Address address,
        Contact contact,
        String email,
        BusinessPartnerType requestedPartnerType,
        SigningRule signingRule,
        List<Signatory> signatories
    ) {
        var pop = new ProofOfPossession("did:example:" + partnerId, UUID.randomUUID().toString());
        pop = pop.toValid();
        return trustOnboardingSubmissionRepository.saveAndFlush(
            new TrustOnboardingSubmission(
                tosId,
                partnerId,
                entityName,
                address,
                email,
                contact,
                Language.DE,
                "TST-111.222.333",
                true,
                List.of(pop),
                requestedPartnerType,
                signingRule,
                signatories,
                Instant.now()
            )
        );
    }

    private void deleteAllDocumentsByPartner(UUID partnerId) {
        var documents = partnerDocumentsRepository.findAllByPartnerId(partnerId);
        for (var document : documents) {
            try {
                partnerDocumentService.deletePartnerDocument(document.getId());
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }
}
