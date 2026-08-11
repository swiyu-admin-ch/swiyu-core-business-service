package ch.admin.bj.swiyu.core.business.modules.dataimport.service;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.CoreDemoData;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.MockMultipartFile;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentsRepository;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntryRepository;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentUploadRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import ch.admin.bj.swiyu.registry.identifier.domain.DatastoreStatus;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntity;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntityRepository;
import java.time.Instant;
import java.util.Arrays;
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
    public void generateBusinessPartners() {
        log.debug("Importing demo business partners...");
        var data = Arrays.stream(CoreDemoData.DemoCase.values())
            .map(demoCase -> DemoDataMapper.toBusinessEntity(demoCase.bp))
            .toList();

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
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateIdentifierEntries() {
        log.debug("Importing demo identifier entries ...");
        Arrays.stream(CoreDemoData.DemoCase.values()).forEach(demoCase ->
            demoCase.bp
                .identifiers()
                .forEach(identifier -> {
                    // get or create Datastore
                    var identifierDatastoreEntity = identifierDatastoreEntityRepository
                        .findById(identifier.id())
                        .orElseGet(() ->
                            identifierDatastoreEntityRepository.save(new IdentifierDatastoreEntity(identifier.id()))
                        );
                    identifierDatastoreEntity.changeStatus(DatastoreStatus.ACTIVE);
                    identifierDatastoreEntityRepository.save(identifierDatastoreEntity);

                    // get or create identifier entry
                    var identifierEntry = identifierEntryRepository
                        .findById(identifier.id())
                        .orElseGet(() ->
                            identifierEntryRepository.save(new IdentifierEntry(identifier.id(), demoCase.bp.id()))
                        );
                    identifierEntry.setDescription(identifier.description());
                    identifierEntryRepository.save(identifierEntry);

                    // add did log
                    if (identifier.data() != null) {
                        identifierEntryService.updateIdentifierEntry(
                            demoCase.bp.id(),
                            identifier.id(),
                            identifier.data()
                        );
                    }
                })
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteDemoTrustOnboardingSubmissions() {
        for (var demoCase : CoreDemoData.DemoCase.values()) {
            deleteAllDocumentsByPartner(demoCase.bp.id());
            trustOnboardingSubmissionRepository.deleteByPartnerId(demoCase.bp.id());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateTrustOnboardingSubmissions() {
        TrustOnboardingSubmission sub;

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUCCEEDED),
            CoreDemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUCCEEDED),
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent.pdf", "something new")
            )
        );
        sub.markAsSucceeded();
        trustOnboardingSubmissionRepository.saveAndFlush(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.DemoCase.GOV_BP_TRUST_ONBOARDING.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUCCEEDED),
            CoreDemoData.DemoCase.GOV_BP_TRUST_ONBOARDING.bp
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.DemoCase.GOV_BP_TRUST_ONBOARDING.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUCCEEDED),
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent.pdf", "something new")
            )
        );
        sub.markAsSucceeded();
        trustOnboardingSubmissionRepository.saveAndFlush(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.DemoCase.BP_GOV_TRUST_ONBOARDING_MORE_INFO.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.INFORMATION_REQUESTED),
            CoreDemoData.DemoCase.BP_GOV_TRUST_ONBOARDING_MORE_INFO.bp
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.DemoCase.BP_GOV_TRUST_ONBOARDING_MORE_INFO.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.INFORMATION_REQUESTED),
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent.pdf", "something different")
            )
        );
        sub.markAsInformationRequested(TrustOnboardingDeclineReason.MISSING_DOCUMENTS, "Test note data");
        trustOnboardingSubmissionRepository.saveAndFlush(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.REJECTED),
            CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp.id(),
            CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp.names(),
            DemoDataMapper.toAddress(CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp.address()),
            DemoDataMapper.toContact(CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp.contact()),
            CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp.email(),
            DemoDataMapper.toBusinessPartnerType(CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp.type()),
            null,
            List.of()
        );
        sub.markAsRejected(TrustOnboardingRejectReason.FRAUDULENT_ACTIVITY);
        trustOnboardingSubmissionRepository.saveAndFlush(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.UNSUBMITTED),
            CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.UNSUBMITTED),
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent from .pdf", "something")
            )
        );
        trustOnboardingSubmissionRepository.saveAndFlush(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUBMITTED),
            CoreDemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUBMITTED),
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
                CoreDemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp
                    .trustOnboardings()
                    .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUBMITTED),
                new TrustOnboardingSubmissionDocumentUploadRequestDto(
                    TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                    new MockMultipartFile(filename, "something with UID")
                )
            );
        }

        sub.markAsSubmitted();
        trustOnboardingSubmissionRepository.save(sub);

        sub = generateTrustOnboardingSubmission(
            CoreDemoData.DemoCase.BP_TRUST_ONBOARDING_OVERDUE.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUBMITTED),
            CoreDemoData.DemoCase.BP_TRUST_ONBOARDING_OVERDUE.bp
        );
        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
            CoreDemoData.DemoCase.BP_TRUST_ONBOARDING_OVERDUE.bp
                .trustOnboardings()
                .get(CoreDemoData.DemoBusinessPartner.DemoTrustOnboardingSubmissionStatus.SUBMITTED),
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                new MockMultipartFile("Declaration of intent.pdf", "something overdue")
            )
        );
        sub.markAsSubmitted();
        trustOnboardingSubmissionRepository.saveAndFlush(sub);
    }

    private TrustOnboardingSubmission generateTrustOnboardingSubmission(
        UUID tosId,
        CoreDemoData.DemoBusinessPartner demoData
    ) {
        return generateTrustOnboardingSubmission(
            tosId,
            demoData.id(),
            demoData.names(),
            DemoDataMapper.toAddress(demoData.address()),
            DemoDataMapper.toContact(demoData.contact()),
            demoData.email(),
            DemoDataMapper.toBusinessPartnerType(demoData.type()),
            DemoDataMapper.toSignatoryRule(demoData.signatoryRule()),
            DemoDataMapper.toSignatoryList(demoData.signatory())
        );
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
                "CHE-123.456.789",
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
