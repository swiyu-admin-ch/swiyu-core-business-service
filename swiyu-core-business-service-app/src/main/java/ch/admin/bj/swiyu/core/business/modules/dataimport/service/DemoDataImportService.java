package ch.admin.bj.swiyu.core.business.modules.dataimport.service;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.DemoData;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.MockMultipartFile;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentsRepository;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntryRepository;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
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
    private final BusinessPartnerService businessPartnerService;
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
        var data = Arrays.stream(DemoData.DemoCase.values())
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
        Arrays.stream(DemoData.DemoCase.values()).forEach(demoCase ->
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
        for (var demoCase : DemoData.DemoCase.values()) {
            deleteAllDocumentsByPartner(demoCase.bp.id());
            trustOnboardingSubmissionRepository.deleteByPartnerId(demoCase.bp.id());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateTrustOnboardingSubmissions() {
        Arrays.stream(DemoData.DemoCase.values()).forEach(demoCase ->
            demoCase.bp
                .trustOnboardings()
                .forEach(trustOnboarding -> {
                    var sub = generateTrustOnboardingSubmission(trustOnboarding.submissionId(), demoCase.bp);
                    for (var demoDocument : trustOnboarding.documents()) {
                        trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
                            trustOnboarding.submissionId(),
                            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                                DemoDataMapper.toTrustOnboardingSubmissionDocumentTypeDto(demoDocument.type()),
                                new MockMultipartFile(demoDocument.fileName(), demoDocument.content())
                            )
                        );
                    }
                    switch (trustOnboarding.status()) {
                        case UNSUBMITTED_TIMEOUT -> sub.markAsExpired();
                        case SUBMITTED -> sub.markAsSubmitted();
                        case INFORMATION_REQUESTED -> sub.markAsInformationRequested("Test note data");
                        case SUCCEEDED -> sub.markAsSucceeded();
                        case REJECTED -> sub.markAsRejected(TrustOnboardingRejectReason.FRAUDULENT_ACTIVITY);
                        case UNSUBMITTED -> {
                            // Nothing to do (but sonar wants all cases handled)
                        }
                    }
                    trustOnboardingSubmissionRepository.saveAndFlush(sub);
                })
        );
    }

    private TrustOnboardingSubmission generateTrustOnboardingSubmission(
        UUID tosId,
        DemoData.DemoBusinessPartner demoData
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

    /**
     * Activates the BusinessPartnerIdentity for demo partners with a SUCCEEDED trust onboarding submission.
     * Normally set via a real TMS BPI event; demo data applies it directly instead, like the other generate*
     * methods here.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateBusinessPartnerIdentities() {
        log.debug("Activating demo business partner identities...");

        Arrays.stream(DemoData.DemoCase.values())
            .filter(demoCase -> demoCase.bp.bpi() != null)
            .forEach(demoCase -> {
                var bpi = BusinessPartnerIdentity.builder()
                    .status(DemoDataMapper.toBusinessPartnerIdentityStatus(demoCase.bp.bpi().status()))
                    .validUntil(demoCase.bp.bpi().validUntil())
                    .uid(demoCase.bp.uid())
                    .entityName(demoCase.bp.names())
                    .trustedIdentifier(
                        demoCase.bp
                            .identifiers()
                            .stream()
                            .filter(DemoData.DemoBusinessPartner.DemoIdentifier::isTrustOnboarded)
                            .map(DemoData.DemoBusinessPartner.DemoIdentifier::did)
                            .toList()
                    )
                    .tmsVersion(0L)
                    .build();
                businessPartnerService.applyBusinessPartnerIdentity(demoCase.bp.id(), bpi);
            });
    }
}
