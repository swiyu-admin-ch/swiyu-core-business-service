package ch.admin.bj.swiyu.core.business.modules.dataimport.service;

import static ch.admin.bj.swiyu.core.business.modules.dataimport.service.DemoDataMapper.toBusinessPartnerIdentityStatus;
import static org.springframework.util.StringUtils.hasText;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.DemoData;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.MockMultipartFile;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentsRepository;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntryRepository;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerIdentityService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentUploadRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityUpdatedPayload;
import ch.admin.bj.swiyu.registry.identifier.domain.DatastoreStatus;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntity;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntityRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;

@SuppressWarnings({ "java:S1192", "java:S5803", "java:S1854" })
@Service
@Profile("test-data-injection")
@RequiredArgsConstructor
@Slf4j
public class DemoDataImportService {

    private final BusinessPartnerRepository businessEntityRepository;
    private final BusinessPartnerIdentityService businessPartnerIdentityService;
    private final IdentifierEntryRepository identifierEntryRepository;
    private final IdentifierDatastoreEntityRepository identifierDatastoreEntityRepository;
    private final PartnerDocumentsRepository partnerDocumentsRepository;
    private final TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;
    private final TrustOnboardingService trustOnboardingService;
    private final ProtectedVerificationSubmissionRepository protectedVerificationSubmissionRepository;
    private final PartnerDocumentService partnerDocumentService;
    private final PamsClient pamsClient;
    private final IdentifierEntryService identifierEntryService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateBusinessPartners() {
        log.debug("Importing demo business partners...");
        var data = Arrays.stream(DemoData.DemoCase.values())
            .map(demoCase -> DemoDataMapper.toBusinessEntity(demoCase.bp))
            .toList();

        // reset the identities before loading the partners, they are re-applied by
        // generateBusinessPartnerIdentities()
        businessPartnerIdentityService.deleteBusinessPartnerIdentities(
            data.stream().map(BusinessEntity::getId).toList()
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
    }

    public void syncBusinessPartnersToPams() {
        var data = Arrays.stream(DemoData.DemoCase.values())
            .map(demoCase -> DemoDataMapper.toBusinessEntity(demoCase.bp))
            .toList();

        for (var d : data) {
            var bp = businessEntityRepository.getReferenceById(d.getId());
            try {
                pamsClient.createBusinessPartner(
                    bp,
                    "8C96FE5C-2F05-4396-41E4-D76D1FB96240" // eid-test-user-1@yopmail.com
                );
            } catch (Exception _) {
                try {
                    pamsClient.updateBusinessPartnerName(bp);
                } catch (Exception e) {
                    log.info("Failure to save Demo BP name to PAMS.", e);
                }
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

                    if (identifier.data() != null) {
                        // Did Option 1/2: a real did log was provided -> use real logic with validation
                        identifierEntryService.updateIdentifierEntry(
                            demoCase.bp.id(),
                            identifier.id(),
                            identifier.data()
                        );
                    } else if (hasText(identifier.did())) {
                        // Did Option 2/2: did was provided but not the log -> take shortcut, we can't validate the log
                        identifierEntry.updateDidAndActivate(identifier.did());
                        identifierEntryRepository.save(identifierEntry);
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
                    var sub = generateTrustOnboardingSubmission(
                        trustOnboarding.submissionId(),
                        demoCase.bp,
                        DemoDataMapper.toTrustOnboardingSubmissionType(trustOnboarding.submissionType())
                    );
                    for (var demoDocument : trustOnboarding.documents()) {
                        if (sub.getType() == TrustOnboardingSubmissionType.RENEWAL) {
                            // not possible with build-in service method due to validation (RENEWAL do not allow upload)
                            uploadPartnerDocument(demoDocument, sub);
                        } else {
                            trustOnboardingService.uploadTrustOnboardingSubmissionDocument(
                                trustOnboarding.submissionId(),
                                new TrustOnboardingSubmissionDocumentUploadRequestDto(
                                    DemoDataMapper.toTrustOnboardingSubmissionDocumentTypeDto(demoDocument.type()),
                                    new MockMultipartFile(demoDocument.fileName(), demoDocument.content())
                                )
                            );
                        }
                    }
                    switch (trustOnboarding.status()) {
                        case UNSUBMITTED_TIMEOUT -> sub.markAsExpired();
                        case SUBMITTED -> sub.markAsSubmitted();
                        case INFORMATION_REQUESTED -> sub.markAsInformationRequested(
                            Instant.now().plus(29, ChronoUnit.DAYS),
                            "Test note data"
                        );
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteDemoProtectedVerificationSubmissions() {
        for (var demoCase : DemoData.DemoCase.values()) {
            protectedVerificationSubmissionRepository.deleteByPartnerId(demoCase.bp.id());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateProtectedVerificationSubmissions() {
        Arrays.stream(DemoData.DemoCase.values()).forEach(demoCase ->
            demoCase.bp
                .protectedVerificationSubmissions()
                .forEach(demoSubmission -> {
                    var sub = new ProtectedVerificationSubmission(
                        demoSubmission.submissionId(),
                        demoCase.bp.id(),
                        demoSubmission.sbnId(),
                        LocalizedMapUtil.getDefaultValue(demoCase.bp.names()),
                        demoCase.bp.uid(),
                        DemoDataMapper.toContact(demoCase.bp.contact()),
                        demoSubmission.reason(),
                        DemoDataMapper.toProtectedVerificationCategory(demoSubmission.category())
                    );
                    switch (demoSubmission.status()) {
                        case APPROVED -> sub.markAsApproved();
                        case REJECTED -> sub.markAsRejected("Test reject reason");
                        case SUBMITTED -> {
                            // Nothing to do, that's the status right after construction
                        }
                    }
                    protectedVerificationSubmissionRepository.saveAndFlush(sub);
                })
        );
    }

    private TrustOnboardingSubmission generateTrustOnboardingSubmission(
        UUID tosId,
        DemoData.DemoBusinessPartner demoData,
        TrustOnboardingSubmissionType submissionType
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
            DemoDataMapper.toSignatoryList(demoData.signatory()),
            submissionType
        );
    }

    private TrustOnboardingSubmission generateTrustOnboardingSubmission( // NOSONAR
        UUID trustOnboardingSubmissionId,
        UUID partnerId,
        Map<String, String> entityName,
        Address address,
        Contact contact,
        String email,
        BusinessPartnerType requestedPartnerType,
        SigningRule signingRule,
        List<Signatory> signatories,
        TrustOnboardingSubmissionType submissionType
    ) {
        var pop = new ProofOfPossession("did:example:" + partnerId, UUID.randomUUID().toString());
        pop = pop.toValid();
        var submission = new TrustOnboardingSubmission(
            submissionType,
            trustOnboardingSubmissionId,
            partnerId,
            entityName,
            address,
            email,
            contact,
            "CHE-123.456.789",
            true,
            List.of(pop),
            requestedPartnerType,
            signingRule,
            signatories
        );
        return trustOnboardingSubmissionRepository.saveAndFlush(submission);
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
            .forEach(demoCase ->
                businessPartnerIdentityService.applyUpdatedBusinessPartnerIdentity(
                    demoCase.bp.id(),
                    new BusinessPartnerIdentityUpdatedPayload(
                        demoCase.bp.id(),
                        demoCase.bp.bpi().validUntil(),
                        demoCase.bp
                            .identifiers()
                            .stream()
                            .filter(DemoData.DemoBusinessPartner.DemoIdentifier::isTrustOnboarded)
                            .map(DemoData.DemoBusinessPartner.DemoIdentifier::did)
                            .toList(),
                        toBusinessPartnerIdentityStatus(demoCase.bp.bpi().status()),
                        demoCase.bp.bpi().validUntil().minus(10, ChronoUnit.DAYS),
                        demoCase.bp.uid(),
                        demoCase.bp.names(),
                        1L
                    )
                )
            );
    }

    /**
     * For certain scenarios it is not possible to use the build-in upload functionality due to validation.
     */
    private void uploadPartnerDocument(
        DemoData.DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument demoDocument,
        TrustOnboardingSubmission submission
    ) {
        var document = partnerDocumentService.createTrustOnboardingSubmissionDocument(
            submission.getPartnerId(),
            submission.getId(),
            DemoDataMapper.toPartnerDocumentTypeDto(demoDocument.type()),
            new MockMultipartFile(demoDocument.fileName(), demoDocument.content())
        );

        if (PartnerDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT.equals(document.type())) {
            submission.updateDeclarationOfIntent(
                new DeclarationOfIntent(document.id(), JsonNodeFactory.instance.objectNode())
            );
            trustOnboardingSubmissionRepository.save(submission);
        }
    }
}
