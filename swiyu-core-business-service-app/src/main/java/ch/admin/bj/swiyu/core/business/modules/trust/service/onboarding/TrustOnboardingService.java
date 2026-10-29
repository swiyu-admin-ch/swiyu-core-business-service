package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper.toAddressDto;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper.toAddressEntity;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerType;
import static ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentMapper.toTrustOnboardingSubmissionDocumentListItemDto;
import static ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper.toAddress;
import static ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper.toLanguage;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustOnboardingMapper.*;

import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.common.email.PendingReviewSubmission;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessDataIntegrityViolationException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ValidationException;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentListItemDto;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerIdentityDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerIdentityStatusDto;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.EventMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustOnboardingMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.*;
import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.OptimisticLockException;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Slf4j
@Service
@AllArgsConstructor
public class TrustOnboardingService {

    /**
     * Time window before a partner's trust identity expiry during which profile changes are governed.
     *
     * <p>Set to 3 calendar years. Using calendar years instead of a fixed 365-day duration avoids
     * leap-year drift, e.g. 3 years is not always exactly 1095 days.
     *
     * @see #validateProfileChangeRespectsTrustExpiration(TrustOnboardingSubmissionTypeDto, BusinessPartnerIdentityDto)
     */
    private static final long PROFILE_CHANGE_THRESHOLD_YEARS = 3;

    private final DomainEventPublisher domainEventPublisher;
    private final TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;
    private final TrustOnboardingSubmissionDomainService trustOnboardingSubmissionDomainService;
    private final BusinessPartnerService businessPartnerService;
    private final TrustOnboardingSubmissionOnSubmitValidator validator;
    private final ProofOfPossessionValidator proofOfPossessionValidator;
    private final PartnerDocumentService partnerDocumentService;
    private final TrustDeclarationOfIntentPdfService trustDeclarationOfIntentPdfService;
    private final TrustOnboardingSubmissionDocumentValidator trustOnboardingSubmissionDocumentValidator;
    private final TrustOnboardingSubmissionValidator trustOnboardingSubmissionValidator;
    private final TrustOnboardingSubmissionLimitProperties limitProperties;
    private final DeclarationOfIntentValidator declarationOfIntentValidator;
    private final AuditPublisher auditPublisher;
    private final EmailCommandPublisher emailCommandPublisher;

    @Transactional(readOnly = true)
    public Resource getDeclarationOfIntentDocument(UUID trustOnboardingSubmissionId, LanguageDto languageDto) {
        var outputStream = new ByteArrayOutputStream();
        trustDeclarationOfIntentPdfService.streamFilledDeclarationOfIntentPdf(
            trustOnboardingSubmissionId,
            Language.valueOf(languageDto.name()),
            outputStream
        );
        return new ByteArrayResource(outputStream.toByteArray());
    }

    @Transactional
    public TrustOnboardingSubmissionDocumentListItemDto uploadTrustOnboardingSubmissionDocument(
        UUID trustOnboardingSubmissionId,
        TrustOnboardingSubmissionDocumentUploadRequestDto request
    ) {
        var submission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );

        trustOnboardingSubmissionDocumentValidator.validateCanUpload(submission, request.file());

        JsonNode doiValidationFileReportAsJson = null;
        if (request.type() == TRUST_ONBOARDING_DECLARATION_OF_INTENT) {
            var result = declarationOfIntentValidator.validateDeclarationOfIntent(
                request.file(),
                submission.getSigningRule()
            );
            doiValidationFileReportAsJson = result.fileReport();
        }

        var document = partnerDocumentService.createTrustOnboardingSubmissionDocument(
            submission.getPartnerId(),
            submission.getId(),
            toPartnerDocumentTypeDto(request.type()),
            request.file()
        );

        if (request.type() == TRUST_ONBOARDING_DECLARATION_OF_INTENT) {
            submission.updateDeclarationOfIntent(new DeclarationOfIntent(document.id(), doiValidationFileReportAsJson));
            trustOnboardingSubmissionRepository.saveAndFlush(submission);
        }

        return toTrustOnboardingSubmissionDocumentListItemDto(document, true);
    }

    @Transactional(readOnly = true)
    public Page<TrustOnboardingSubmissionDocumentListItemDto> findAllDocumentsByTrustOnboardingSubmissionId(
        UUID trustOnboardingSubmissionId,
        Pageable pageable
    ) {
        var submission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        var canBeDeleted = !trustOnboardingSubmissionValidator
            .validateTrustOnboardingSubmissionCanBeEdited(submission, null)
            .hasErrors();
        return partnerDocumentService
            .findAllByTrustOnboardingSubmissionId(trustOnboardingSubmissionId, pageable)
            .map(doc -> toTrustOnboardingSubmissionDocumentListItemDto(doc, canBeDeleted));
    }

    @Transactional
    public void deleteTrustOnboardingSubmissionDocument(UUID trustOnboardingSubmissionId, UUID documentId) {
        var submission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        trustOnboardingSubmissionValidator.validateCanDelete(submission);
        if (!partnerDocumentService.isDocumentBelongingToSubmission(documentId, trustOnboardingSubmissionId)) {
            throw new AuthorizationDeniedException(
                "Document '%s' does not belong to trust onboarding submission '%s'".formatted(
                    documentId,
                    trustOnboardingSubmissionId
                )
            );
        }
        partnerDocumentService.deletePartnerDocument(documentId);
        // If the deleted document was the signed DOI, clear the reference on the submission.
        submission.removeDeclarationOfIntent(documentId);
        trustOnboardingSubmissionRepository.save(submission);
    }

    @Transactional
    public TrustOnboardingSubmissionDto createTrustOnboardingSubmission(TrustOnboardingSubmissionRequestDto request) {
        // if partner already has an ongoing submission return it instead of creating a new one
        TrustOnboardingSubmission inProgressEntry = trustOnboardingSubmissionRepository.findByPartnerIdAndStatusIn(
            request.partnerId(),
            List.of(
                TrustOnboardingSubmissionStatus.SUBMITTED,
                TrustOnboardingSubmissionStatus.UNSUBMITTED,
                TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED,
                TrustOnboardingSubmissionStatus.RESUBMITTED
            )
        );
        var submissionType =
            request.submissionType() != null ? request.submissionType() : TrustOnboardingSubmissionTypeDto.REGISTRATION;
        validateIsExpectedSubmissionType(submissionType, inProgressEntry);
        if (inProgressEntry != null) {
            // tech debt: the functionality should be split towards create and update, but therefore some EMC with portal UI is needed
            log.info(
                "Did not create TrustOnboardingSubmission for partner {} because there was already one in progress. Returning this one instead.",
                request.partnerId()
            );
            return toTrustOnboardingsSubmissionDto(inProgressEntry);
        }

        TrustOnboardingSubmission submission;
        if (submissionType.isProfileChangeOrRenewal()) {
            submission = createProfileChangeOrRenewalSubmission(request, submissionType);
        } else {
            submission = createRegistrationSubmission(request);
        }
        return toTrustOnboardingsSubmissionDto(submission);
    }

    @Transactional(readOnly = true)
    public TrustOnboardingSubmissionDto getTrustOnboardingSubmission(UUID trustOnboardingSubmissionId) {
        return toTrustOnboardingsSubmissionDto(
            trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(trustOnboardingSubmissionId)
        );
    }

    @Transactional(readOnly = true)
    public TrustOnboardingSubmissionDto getSubmissionByPartnerId(UUID partnerId) {
        return toTrustOnboardingsSubmissionDto(
            trustOnboardingSubmissionDomainService.getUnsubmittedTrustOnboardingSubmissionByPartner(partnerId)
        );
    }

    @Transactional
    public TrustOnboardingSubmissionDto updateTrustOnboardingSubmission(
        UUID trustOnboardingSubmissionId,
        TrustOnboardingSubmissionRequestDto request
    ) {
        var submission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );

        // When the user starts making changes again after more information was requested,
        // the submission is moved back to UNSUBMITTED (see EID-6376).
        // RESUBMITTED is deliberately NOT included: it behaves like SUBMITTED (locked, no further changes).
        var status = submission.getStatus();
        if (status == TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED) {
            submission.markAsUnsubmitted();
        }

        String uid = null;
        if (request.hasUid()) {
            uid = request.registryIds().get("UID");
        }

        // Determine before update whether DOI-relevant fields are changing
        boolean discardDoi = hasDoiRelevantChanges(submission, request, uid);

        var popList = submission.getProofOfPossessions();
        if (!proofOfPossessionValidator.isDidSelectionEqual(popList, request.dids())) {
            popList = TrustOnboardingMapper.toProofOfPossession(request.dids());
        }

        switch (submission.getType()) {
            case REGISTRATION -> submission.updateForTypeRegistration(
                request.entityName(),
                toAddressEntity(request.entityAddress()),
                request.entityEmail(),
                toContactEntity(request.contactPerson()),
                uid,
                popList,
                toBusinessPartnerType(request.requestedPartnerType()),
                toSigningRule(request.signingRule()),
                toSignatories(request.signatories()),
                Boolean.TRUE.equals(request.isRegisteredInCommercialRegister())
            );
            case PROFILE_CHANGE_MANDATORY, PROFILE_CHANGE_VOLUNTARY -> submission.updateForTypeProfileChange(
                request.entityName(),
                toAddressEntity(request.entityAddress()),
                request.entityEmail(),
                toContactEntity(request.contactPerson()),
                popList,
                toBusinessPartnerType(request.requestedPartnerType()),
                toSigningRule(request.signingRule()),
                toSignatories(request.signatories()),
                Boolean.TRUE.equals(request.isRegisteredInCommercialRegister())
            );
            case RENEWAL -> submission.updateForTypeRenewal(
                toAddressEntity(request.entityAddress()),
                request.entityEmail(),
                toContactEntity(request.contactPerson()),
                popList
            );
        }

        // If DOI-relevant fields changed, the signed DOI is no longer valid and must be deleted.
        // Capture the ID before nulling the reference — we need it for the S3 delete after flush.
        UUID doiDocumentIdToDelete = null;
        if (discardDoi) {
            doiDocumentIdToDelete = discardDeclarationOfIntend(submission, doiDocumentIdToDelete);
        }

        // Flush first: if this throws (e.g. OptimisticLockException), S3 is still untouched.
        var result = trustOnboardingSubmissionRepository.saveAndFlush(submission);

        // Delete from S3+DB only after the flush succeeds.
        if (doiDocumentIdToDelete != null) {
            partnerDocumentService.deletePartnerDocument(doiDocumentIdToDelete);
        }

        return toTrustOnboardingsSubmissionDto(result);
    }

    @Transactional
    public TrustOnboardingSubmissionDto submitProofOfPossessions(UUID partnerId, List<String> rawProofOfPossessions) {
        var submission = trustOnboardingSubmissionDomainService.getUnsubmittedTrustOnboardingSubmissionByPartner(
            partnerId
        );

        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(
            rawProofOfPossessions,
            submission.getProofOfPossessions()
        );

        if (errors.hasErrors()) {
            throw new ValidationException("Provided proof of possessions are not valid", errors);
        }

        var validPops = submission.getProofOfPossessions().stream().map(ProofOfPossession::toValid).toList();

        submission.submitProofOfPossessions(validPops);
        return toTrustOnboardingsSubmissionDto(submission);
    }

    @Transactional(readOnly = true)
    public Page<TrustOnboardingSubmissionListItemDto> getAllTrustOnboardings(
        TrustOnboardingSubmissionFilterDto filter,
        Pageable pageable
    ) {
        // need to filter to only return submissions the user has access to. EID-5480

        var q = QTrustOnboardingSubmission.trustOnboardingSubmission;
        var where = new BooleanBuilder();
        if (filter.businessPartnerIds() != null) {
            where.and(q.partnerId.in(filter.businessPartnerIds()));
        }

        return trustOnboardingSubmissionRepository
            .findAll(
                where,
                PageableUtils.toDbPageableFromUserPageable(
                    TrustOnboardingSubmissionDto.class,
                    TrustOnboardingSubmission.class,
                    pageable
                )
            )
            .map(TrustOnboardingMapper::toTrustOnboardingSubmissionListItemDto);
    }

    @Transactional
    public void submit(UUID trustOnboardingSubmissionId, TrustOnboardingSubmitRequestDto request) {
        var submission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );

        if (!submission.getVersion().equals(request.getVersion())) {
            throw new OptimisticLockException(
                "Version mismatch " + submission.getVersion() + " != " + request.getVersion()
            );
        }

        var businessPartnerType = businessPartnerService.getBusinessPartnerType(submission.getPartnerId());

        validator.validateCanSubmit(submission, businessPartnerType);

        submission.markAsSubmitted();
        submission = trustOnboardingSubmissionRepository.saveAndFlush(submission);
        var doiS3Keys = partnerDocumentService.getAllStorageKeysByTrustOnboardingSubmissionId(submission.getId());
        auditPublisher.trustOnboardingSubmitted(
            submission.getId().toString(),
            String.valueOf(submission.getVersion()),
            submission.getPartnerId().toString(),
            AuditMapper.toAuditJson(submission),
            doiS3Keys
        );
        domainEventPublisher.publishTiTrustOnboardingSubmissionAcceptedEvent(
            EventMapper.mapToTiTrustOnboardingSubmissionAcceptedEvent(submission.getId(), submission.getPartnerId())
        );
        var partnerId = submission.getPartnerId();
        emailCommandPublisher.submissionAccepted(partnerId);
    }

    @Transactional
    public void markAsRejected(UUID trustOnboardingSubmissionId, String rejectReason) {
        var trustOnboardingSubmission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        trustOnboardingSubmission.markAsRejected(toTrustOnboardingRejectReason(rejectReason));

        publishSubmissionOutcomeEmail(trustOnboardingSubmission, Outcome.REJECTED);
    }

    @Transactional
    public void markAsInformationRequested(
        UUID trustOnboardingSubmissionId,
        Instant resubmitRequiredUntil,
        String partnerNote
    ) {
        var trustOnboardingSubmission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        trustOnboardingSubmission.markAsInformationRequested(resubmitRequiredUntil, partnerNote);

        publishSubmissionOutcomeEmail(trustOnboardingSubmission, Outcome.INFORMATION_REQUESTED);
    }

    @Transactional
    public void markAsSucceeded(UUID trustOnboardingSubmissionId) {
        var trustOnboardingSubmission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        trustOnboardingSubmission.markAsSucceeded();

        updateBusinessPartnerWithSubmissionDetails(trustOnboardingSubmission);

        publishSubmissionOutcomeEmail(trustOnboardingSubmission, Outcome.SUCCEEDED);
    }

    /**
     * Submissions that have been waiting for review longer than {@code minDelay}, one page at a time.
     *
     * <p>For the nightly "review delayed" reminder. Paged for the same reason as the renewal query: the
     * number of open submissions is not bounded by anything the code controls.
     */
    @Transactional(readOnly = true)
    public Page<PendingReviewSubmission> findSubmissionsPendingReviewLongerThan(Duration minDelay, Pageable pageable) {
        var cutoff = Instant.now().minus(minDelay);
        return trustOnboardingSubmissionRepository.findPendingReviewSubmittedBefore(cutoff, pageable);
    }

    @Transactional
    public void trustOnboardingSubmissionCheckForUnsubmittedTimeout() {
        // To assert that the lock is held (prevents misconfiguration errors)
        LockAssert.assertLocked();

        var maxAgeTimestamp = Instant.now().minus(limitProperties.maxAgeInUnsubmitted());
        log.debug("Checking for TrustOnboardingSubmissions expiry with maxAgeTimestamp: {}", maxAgeTimestamp);
        var editedRows = trustOnboardingSubmissionRepository.updateStatusToTimeout(maxAgeTimestamp);
        log.info("{} TrustOnboardingSubmissions did expire.", editedRows);
    }

    @Transactional
    public void hardDeleteByPartnerId(UUID partnerId) {
        log.info("Hard deleting all TrustOnboardingSubmissions of business partner '{}'", partnerId);
        trustOnboardingSubmissionRepository.deleteByPartnerId(partnerId);
    }

    private static UUID discardDeclarationOfIntend(TrustOnboardingSubmission submission, UUID doiDocumentIdToDelete) {
        var doi = submission.getDeclarationOfIntent();
        if (doi != null) {
            doiDocumentIdToDelete = doi.getFullySignedDocumentId();
            submission.removeDeclarationOfIntent(doiDocumentIdToDelete);
        }
        return doiDocumentIdToDelete;
    }

    private void validateIsExpectedSubmissionType(
        TrustOnboardingSubmissionTypeDto expectedSubmissionType,
        TrustOnboardingSubmission submission
    ) {
        if (submission == null) {
            return;
        }
        if (!toTrustOnboardingSubmissionType(expectedSubmissionType).equals(submission.getType())) {
            var msg =
                "Can't create submission of type '%s', since there is already a submission in progress with type '%s'.".formatted(
                    expectedSubmissionType,
                    submission.getType()
                );
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    private TrustOnboardingSubmission createRegistrationSubmission(TrustOnboardingSubmissionRequestDto request) {
        TrustOnboardingSubmission submission;
        String uid = null;
        if (request.registryIds() != null && request.registryIds().containsKey("UID")) {
            uid = request.registryIds().get("UID");
        }
        submission = trustOnboardingSubmissionRepository.save(
            new TrustOnboardingSubmission(
                UUID.randomUUID(),
                request.partnerId(),
                request.entityName(),
                toAddressEntity(request.entityAddress()),
                request.entityEmail(),
                toContactEntity(request.contactPerson()),
                uid,
                true,
                toProofOfPossession(request.dids()),
                toBusinessPartnerType(request.requestedPartnerType()),
                toSigningRule(request.signingRule()),
                toSignatories(request.signatories())
            )
        );
        return submission;
    }

    /**
     * Creates a PROFILE_CHANGE or RENEWAL submission derived entirely from the verified partner data.
     * Disregards all request values except partnerId and submissionType.
     *
     * @param request the request with partnerId and other params required to create a submission
     * @param submissionType must be PROFILE_CHANGE_MANDATORY, PROFILE_CHANGE_VOLUNTARY, or RENEWAL
     * @return the created submission DTO
     */
    private TrustOnboardingSubmission createProfileChangeOrRenewalSubmission(
        TrustOnboardingSubmissionRequestDto request,
        TrustOnboardingSubmissionTypeDto submissionType
    ) {
        var businessPartner = businessPartnerService.getBusinessPartner(request.partnerId());
        var bpi = businessPartner.businessPartnerIdentity();

        // Get the BusinessPartnerIdentity
        if (bpi == null) {
            throw new BusinessDataIntegrityViolationException("Partner has no BusinessPartnerIdentity.");
        }

        // Validate partner has ACTIVE BusinessPartnerIdentity (is trusted)
        if (bpi.status() != BusinessPartnerIdentityStatusDto.ACTIVE) {
            throw new BusinessDataIntegrityViolationException(
                "Partner is not trusted (no ACTIVE BusinessPartnerIdentity)."
            );
        }

        validateProfileChangeRespectsTrustExpiration(submissionType, bpi);

        // Fetch the latest SUCCEEDED submission to derive signingRule, signatories
        var latest = trustOnboardingSubmissionRepository
            .findLatestSucceededByPartnerId(request.partnerId())
            .orElseThrow(() ->
                new BusinessDataIntegrityViolationException(
                    "Failed to create a submission of type %s because no previous succeeded submission was found".formatted(
                        submissionType
                    )
                )
            );

        var pops = toProofOfPossession(request.dids());
        if (submissionType.isProfileChangeOrRenewal()) {
            // on any renewal or profile change -> the selected dids are the already trusted dids
            pops = createProofOfPossessionsFromAlreadyTrustedDids(bpi);
        }

        // Correspondence language comes from contactPerson on BusinessEntity. It defaults to DE
        // because the trust management service reads a missing language as EN_CH.
        var contact = businessPartner.contact();
        Contact contactPerson = null;
        if (contact != null) {
            var language = contact.correspondingLanguage();
            contactPerson = new Contact(
                contact.firstName(),
                contact.lastName(),
                contact.email(),
                contact.phone(),
                language != null ? toLanguage(language) : Language.DE
            );
        }

        // Derive signingRule and signatories from latest submission
        var signingRule = latest.getSigningRule();
        var signatories = latest.getSignatories();

        // Create the submission with type set in constructor via updateType before first save
        var submission = new TrustOnboardingSubmission(
            toTrustOnboardingSubmissionType(submissionType),
            UUID.randomUUID(),
            request.partnerId(),
            bpi.entityName(),
            toAddress(businessPartner.address()),
            contact != null ? contact.email() : null,
            contactPerson,
            bpi.uid(),
            false, // isRegisteredInCommercialRegister not applicable for profile change/renewal
            pops,
            toBusinessPartnerType(businessPartner.type()),
            signingRule,
            signatories
        );
        submission = trustOnboardingSubmissionRepository.saveAndFlush(submission);

        if (submission.isRenewal()) {
            copyDocumentsAndDoiStateFromLatestSubmission(submission, latest, request.partnerId());
        }
        return submission;
    }

    /**
     * Copies the signed Declaration of Intent from the latest submission onto the given RENEWAL submission.
     * In the renewal flow these documents will be submitted again.
     *
     * @throws BusinessDataIntegrityViolationException if the previous submission has no Declaration of Intent (it then could
     *                               never have been submitted) or if the copied DOI document is missing.
     */
    private void copyDocumentsAndDoiStateFromLatestSubmission(
        TrustOnboardingSubmission submission,
        TrustOnboardingSubmission latest,
        UUID partnerId
    ) {
        if (latest.getDeclarationOfIntent() == null) {
            throw new BusinessDataIntegrityViolationException(
                "Can not create TrustOnboardingSubmission of type Renewal, because the previous Submission has no Declaration of Intent"
            );
        }
        var copiedDocuments = partnerDocumentService.copyTrustOnboardingSubmissionDocuments(
            latest.getId(),
            submission.getId(),
            partnerId
        );
        var copiedDoiDocument = copiedDocuments
            .stream()
            .filter(doc -> doc.type() == PartnerDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT)
            .findFirst()
            .orElseThrow(() ->
                new BusinessDataIntegrityViolationException(
                    "Can not create TrustOnboardingSubmission of type Renewal, because the DOI document is missing in previous submission"
                )
            );
        submission.updateDeclarationOfIntent(
            new DeclarationOfIntent(copiedDoiDocument.id(), latest.getDeclarationOfIntent().getValidationReport())
        );
        trustOnboardingSubmissionRepository.save(submission);
    }

    private void validateProfileChangeRespectsTrustExpiration(
        TrustOnboardingSubmissionTypeDto submissionType,
        BusinessPartnerIdentityDto bpi
    ) {
        if (
            submissionType != TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_MANDATORY &&
            submissionType != TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_VOLUNTARY
        ) {
            return;
        }

        var trustIdentityExpiry = bpi.validUntil();
        if (trustIdentityExpiry == null) {
            throw new BusinessDataIntegrityViolationException("Partner has no trust identity expiry date.");
        }
        var maxExpiry = Instant.now()
            .atOffset(ZoneOffset.UTC)
            .toLocalDateTime()
            .plusYears(PROFILE_CHANGE_THRESHOLD_YEARS)
            .toInstant(ZoneOffset.UTC);
        var isWithinThreshold = trustIdentityExpiry.isBefore(maxExpiry) || trustIdentityExpiry.equals(maxExpiry);
        if (submissionType == TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_MANDATORY && !isWithinThreshold) {
            log.warn(
                "Mandatory profile change attempted outside of 3 years threshold for trust identity expiry: {}",
                trustIdentityExpiry
            );
        }
    }

    /**
     * Build ProofOfPossession from trustedIdentifier (List<String> of DIDs) with status NOT_SUPPLIED. Users need
     * to re-do the proof-of-possession.
     */
    private List<ProofOfPossession> createProofOfPossessionsFromAlreadyTrustedDids(BusinessPartnerIdentityDto bpi) {
        if (bpi.trustedIdentifier() == null) {
            return List.of();
        }
        return bpi
            .trustedIdentifier()
            .stream()
            .map(did -> new ProofOfPossession(did, UUID.randomUUID().toString()).toNotSupplied())
            .toList();
    }

    /**
     * Returns true if any field that is reflected in the Declaration of Intent has changed.
     * Affected fields: UID, organisation name, address, DIDs (proof of possessions), signing rule, signatories.
     *
     * <p>Internally collects the keys of all changed fields into a list, which is logged for traceability.
     * If the list is empty no DOI-relevant field changed, so {@code false} is returned.
     */
    private boolean hasDoiRelevantChanges(
        TrustOnboardingSubmission submission,
        TrustOnboardingSubmissionRequestDto request,
        String newUid
    ) {
        var changedFields = new ArrayList<String>();

        if (!Objects.equals(submission.getUid(), newUid)) {
            changedFields.add("uid");
        }
        if (!Objects.equals(submission.getEntityName(), request.entityName())) {
            changedFields.add("entityName");
        }
        // edge case renewal: address can be changed when it is a RENEWAL submission
        if (
            !submission.isRenewal() &&
            !Objects.equals(toAddressDto(submission.getEntityAddress()), request.entityAddress())
        ) {
            changedFields.add("entityAddress");
        }
        if (!proofOfPossessionValidator.isDidSelectionEqual(submission.getProofOfPossessions(), request.dids())) {
            changedFields.add("dids");
        }
        if (!Objects.equals(submission.getSigningRule(), toSigningRule(request.signingRule()))) {
            changedFields.add("signingRule");
        }
        if (!Objects.equals(submission.getSignatories(), toSignatories(request.signatories()))) {
            changedFields.add("signatories");
        }
        if (changedFields.isEmpty()) {
            return false;
        }
        log.debug("DOI-relevant fields changed for submission {}: {}", submission.getId(), changedFields);
        if (submission.isRenewal()) {
            log.error(
                "Relevant fields changed ({}) of the submission {} which is in type RENEWAL. This should not happen",
                changedFields,
                submission.getId()
            );
        }
        return true;
    }

    private void updateBusinessPartnerWithSubmissionDetails(TrustOnboardingSubmission trustOnboardingSubmission) {
        var entityName = trustOnboardingSubmission.getEntityName();
        businessPartnerService.updateBusinessPartner(
            trustOnboardingSubmission.getPartnerId(),
            entityName,
            trustOnboardingSubmission.getEntityAddress(),
            trustOnboardingSubmission.getEntityEmail(),
            trustOnboardingSubmission.getUid(),
            trustOnboardingSubmission.getContactPerson().getPhone(),
            trustOnboardingSubmission.getRequestedPartnerType()
        );
    }

    private void publishSubmissionOutcomeEmail(TrustOnboardingSubmission submission, Outcome outcome) {
        var partnerId = submission.getPartnerId();
        switch (submission.getType()) {
            case REGISTRATION -> publishOutcomeEmail(
                outcome,
                () -> emailCommandPublisher.trustRegistrationSucceeded(partnerId),
                () -> emailCommandPublisher.trustRegistrationRejected(partnerId),
                () -> emailCommandPublisher.trustRegistrationInformationRequested(partnerId)
            );
            case PROFILE_CHANGE_MANDATORY, PROFILE_CHANGE_VOLUNTARY -> publishOutcomeEmail(
                outcome,
                () -> emailCommandPublisher.trustProfileChangeSucceeded(partnerId),
                () -> emailCommandPublisher.trustProfileChangeRejected(partnerId),
                () -> emailCommandPublisher.trustProfileChangeInformationRequested(partnerId)
            );
            case RENEWAL ->
                // No trustRenewalRejected / trustRenewalInformationRequested variant exists yet; fall back to the
                // PROFILE_CHANGE variants. See EID-6620 Open Questions.
                publishOutcomeEmail(
                    outcome,
                    () -> emailCommandPublisher.trustRenewalSucceeded(partnerId),
                    () -> emailCommandPublisher.trustProfileChangeRejected(partnerId),
                    () -> emailCommandPublisher.trustProfileChangeInformationRequested(partnerId)
                );
        }
    }

    private void publishOutcomeEmail(
        Outcome outcome,
        Runnable succeeded,
        Runnable rejected,
        Runnable informationRequested
    ) {
        if (outcome == Outcome.SUCCEEDED) succeeded.run();
        if (outcome == Outcome.REJECTED) rejected.run();
        if (outcome == Outcome.INFORMATION_REQUESTED) informationRequested.run();
    }

    private enum Outcome {
        SUCCEEDED,
        REJECTED,
        INFORMATION_REQUESTED,
    }

    private TrustOnboardingSubmissionDto toTrustOnboardingsSubmissionDto(
        TrustOnboardingSubmission trustOnboardingSubmission
    ) {
        return TrustOnboardingMapper.toTrustOnboardingSubmissionDto(trustOnboardingSubmission);
    }
}
