package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

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
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ValidationException;
import ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import tools.jackson.databind.JsonNode;

@Slf4j
@Service
@AllArgsConstructor
public class TrustOnboardingService {

    private static final Duration RENEWAL_THRESHOLD = Duration.ofDays(365L * 3);

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
        var trustOnboarding = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );

        var errors = trustOnboardingSubmissionDocumentValidator.validateDocument(trustOnboarding, request.file(), null);

        if (errors.hasErrors()) {
            throw new ValidationException("TrustOnboardingSubmissionDocument has missing/invalid fields.", errors);
        }

        JsonNode doiValidationFileReportAsJson = null;
        if (request.type() == TRUST_ONBOARDING_DECLARATION_OF_INTENT) {
            var result = declarationOfIntentValidator.validateDeclarationOfIntent(
                request.file(),
                trustOnboarding.getSigningRule()
            );
            doiValidationFileReportAsJson = result.fileReport();
        }

        var document = partnerDocumentService.createTrustOnboardingSubmissionDocument(
            trustOnboarding.getPartnerId(),
            trustOnboarding.getId(),
            TrustOnboardingMapper.toPartnerDocumentTypeDto(request.type()),
            request.file()
        );

        var submissionDocument = toTrustOnboardingSubmissionDocumentListItemDto(document, true);

        if (request.type() == TRUST_ONBOARDING_DECLARATION_OF_INTENT) {
            trustOnboarding.updateDeclarationOfIntent(
                new DeclarationOfIntent(document.id(), doiValidationFileReportAsJson)
            );
            trustOnboardingSubmissionRepository.saveAndFlush(trustOnboarding);
        }

        return submissionDocument;
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
        if (submission.getType() == TrustOnboardingSubmissionType.RENEWAL) {
            throw new ValidationException(
                "Documents cannot be deleted from a RENEWAL submission.",
                new BeanPropertyBindingResult(submission, "trustOnboardingSubmission")
            );
        }
        var errors = trustOnboardingSubmissionValidator.validateTrustOnboardingSubmissionCanBeEdited(submission, null);
        if (errors.hasErrors()) {
            throw new ValidationException("Submission cannot be edited.", errors);
        }
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
    public TrustOnboardingSubmissionDto createTrustOnboardingSubmission(TrustOnboardingSubmissionRequestDto dto) {
        // if partner already has an ongoing submission return it instead of creating a new one
        TrustOnboardingSubmission inProgressEntry = trustOnboardingSubmissionRepository.findByPartnerIdAndStatusIn(
            dto.partnerId(),
            List.of(
                TrustOnboardingSubmissionStatus.SUBMITTED,
                TrustOnboardingSubmissionStatus.UNSUBMITTED,
                TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED,
                TrustOnboardingSubmissionStatus.RESUBMITTED
            )
        );
        if (inProgressEntry != null) {
            return toTrustOnboardingsSubmissionDto(inProgressEntry);
        }

        var submissionType =
            dto.submissionType() != null ? dto.submissionType() : TrustOnboardingSubmissionTypeDto.REGISTRATION;

        // For PROFILE_CHANGE and RENEWAL, derive everything from verified partner data
        if (
            submissionType == TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_MANDATORY ||
            submissionType == TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_VOLUNTARY ||
            submissionType == TrustOnboardingSubmissionTypeDto.RENEWAL
        ) {
            return createProfileChangeOrRenewalSubmission(dto.partnerId(), submissionType);
        }

        // REGISTRATION: use request data
        String uid = null;
        if (dto.registryIds() != null && dto.registryIds().containsKey("UID")) {
            uid = dto.registryIds().get("UID");
        }

        TrustOnboardingSubmission trustOnboardingSubmission = trustOnboardingSubmissionRepository.save(
            new TrustOnboardingSubmission(
                UUID.randomUUID(),
                dto.partnerId(),
                dto.entityName(),
                AddressMapper.toAddressEntity(dto.entityAddress()),
                dto.entityEmail(),
                toContactEntity(dto.contactPerson()),
                uid,
                true,
                TrustOnboardingMapper.toProofOfPossession(dto.dids()),
                toBusinessPartnerType(dto.requestedPartnerType()),
                toSigningRule(dto.signingRule()),
                toSignatories(dto.signatories())
            )
        );
        return toTrustOnboardingsSubmissionDto(trustOnboardingSubmission);
    }

    /**
     * Creates a PROFILE_CHANGE or RENEWAL submission derived entirely from the verified partner data.
     * Disregards all request values except partnerId and submissionType.
     *
     * @param partnerId the partner ID
     * @param submissionType must be PROFILE_CHANGE_MANDATORY, PROFILE_CHANGE_VOLUNTARY, or RENEWAL
     * @return the created submission DTO
     */
    private TrustOnboardingSubmissionDto createProfileChangeOrRenewalSubmission(
        UUID partnerId,
        TrustOnboardingSubmissionTypeDto submissionType
    ) {
        var businessPartner = businessPartnerService.getBusinessPartner(partnerId);
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

        validateProfileChangeRule(submissionType, bpi);

        // Fetch the latest submission to derive signingRule, signatories, and PoP nonces
        var latest = trustOnboardingSubmissionRepository
            .findAllByPartnerIdOrderByInitiatedAtAsc(partnerId)
            .stream()
            .reduce((a, b) -> b)
            .orElseThrow(() -> new ResourceNotFoundException("No existing submission for partner."));

        // Build ProofOfPossession from trustedIdentifier (List<String> of DIDs) with status VALID
        // Preserve nonces from latest submission for DIDs that already existed
        var pops = buildProofOfPossessions(bpi, latest);

        // Correspondence language comes from contactPerson on BusinessEntity. It defaults to DE
        // because the trust management service reads a missing language as EN_CH.
        var contact = businessPartner.contact();
        Contact contactPerson = null;
        if (contact != null) {
            var language = contact.correspondingLanguage();
            contactPerson = Contact.builder()
                .firstName(contact.firstName())
                .lastName(contact.lastName())
                .email(contact.email())
                .phone(contact.phone())
                .correspondingLanguage(language != null ? toLanguage(language) : Language.DE)
                .build();
        }

        // Derive signingRule and signatories from latest submission
        var signingRule = latest.getSigningRule();
        var signatories = latest.getSignatories();

        // Create the submission with type set in constructor via updateType before first save
        var submission = new TrustOnboardingSubmission(
            toTrustOnboardingSubmissionType(submissionType),
            UUID.randomUUID(),
            partnerId,
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
        var saved = trustOnboardingSubmissionRepository.saveAndFlush(submission);

        // Copy documents from latest submission
        partnerDocumentService.copyTrustOnboardingSubmissionDocuments(latest.getId(), saved.getId(), partnerId);

        return toTrustOnboardingsSubmissionDto(saved);
    }

    private void validateProfileChangeRule(
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
        var timeUntilExpiry = Duration.between(Instant.now(), trustIdentityExpiry);
        var isWithinThreshold = timeUntilExpiry.compareTo(RENEWAL_THRESHOLD) <= 0;

        if (submissionType == TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_MANDATORY && !isWithinThreshold) {
            throw new BusinessDataIntegrityViolationException(
                "Mandatory profile change only allowed within 3 years of trust identity expiry."
            );
        }
        if (submissionType == TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_VOLUNTARY && isWithinThreshold) {
            throw new BusinessDataIntegrityViolationException(
                "Voluntary profile change only allowed outside 3 years of trust identity expiry."
            );
        }
    }

    /*
     * Build ProofOfPossession from trustedIdentifier (List<String> of DIDs) with status VALID
     * Preserve nonces from latest submission for DIDs that already existed
     */
    private List<ProofOfPossession> buildProofOfPossessions(
        BusinessPartnerIdentityDto bpi,
        TrustOnboardingSubmission latest
    ) {
        Map<String, String> existingNonces =
            latest.getProofOfPossessions() == null
                ? Map.of()
                : latest
                      .getProofOfPossessions()
                      .stream()
                      .collect(Collectors.toMap(ProofOfPossession::getDid, ProofOfPossession::getNonce, (a, b) -> a));
        if (bpi.trustedIdentifier() == null) {
            return List.of();
        }
        return bpi
            .trustedIdentifier()
            .stream()
            .map(did -> {
                String nonce = existingNonces.getOrDefault(did, UUID.randomUUID().toString());
                return new ProofOfPossession(did, nonce).toValid();
            })
            .toList();
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
        TrustOnboardingSubmissionRequestDto dto
    ) {
        var trustOnboardingSubmission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );

        // When the user starts making changes again after more information was requested,
        // the submission is moved back to UNSUBMITTED (see EID-6376).
        // RESUBMITTED is deliberately NOT included: it behaves like SUBMITTED (locked, no further changes).
        var status = trustOnboardingSubmission.getStatus();
        if (status == TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED) {
            trustOnboardingSubmission.markAsUnsubmitted();
        }

        String uid = null;
        if (dto.registryIds() != null && dto.registryIds().containsKey("UID")) {
            uid = dto.registryIds().get("UID");
        }

        // For RENEWAL and PROFILE_CHANGE submissions, restrict updatable fields
        var type = trustOnboardingSubmission.getType();
        var isRenewal = type == TrustOnboardingSubmissionType.RENEWAL;
        var isProfileChange =
            type == TrustOnboardingSubmissionType.PROFILE_CHANGE_MANDATORY ||
            type == TrustOnboardingSubmissionType.PROFILE_CHANGE_VOLUNTARY;

        // UID is always fixed for restricted types
        if (isRenewal || isProfileChange) {
            uid = trustOnboardingSubmission.getUid();
        }

        // For RENEWAL: entityName, signingRule, signatories, commercialRegister are also fixed
        Map<String, String> entityName = isRenewal ? trustOnboardingSubmission.getEntityName() : dto.entityName();
        SigningRule signingRule = isRenewal
            ? trustOnboardingSubmission.getSigningRule()
            : toSigningRule(dto.signingRule());
        List<Signatory> signatories = isRenewal
            ? trustOnboardingSubmission.getSignatories()
            : toSignatories(dto.signatories());
        Boolean commercialRegister = isRenewal
            ? trustOnboardingSubmission.getIsRegisteredInCommercialRegister()
            : Boolean.TRUE.equals(dto.isRegisteredInCommercialRegister());

        // Determine before update whether DOI-relevant fields are changing
        boolean discardDoi = hasDoiRelevantChanges(trustOnboardingSubmission, dto, uid);

        var popList = trustOnboardingSubmission.getProofOfPossessions();
        if (!proofOfPossessionValidator.isDidSelectionEqual(popList, dto.dids())) {
            popList = TrustOnboardingMapper.toProofOfPossession(dto.dids());
        }

        trustOnboardingSubmission.update(
            entityName,
            AddressMapper.toAddressEntity(dto.entityAddress()),
            dto.entityEmail(),
            toContactEntity(dto.contactPerson()),
            uid,
            popList,
            toBusinessPartnerType(dto.requestedPartnerType()),
            signingRule,
            signatories,
            commercialRegister
        );

        // If DOI-relevant fields changed, the signed DOI is no longer valid and must be deleted.
        // Capture the ID before nulling the reference — we need it for the S3 delete after flush.
        UUID doiDocumentIdToDelete = null;
        if (discardDoi) {
            var doi = trustOnboardingSubmission.getDeclarationOfIntent();
            if (doi != null) {
                doiDocumentIdToDelete = doi.getFullySignedDocumentId();
                trustOnboardingSubmission.updateDeclarationOfIntent(null);
            }
        }

        // Flush first: if this throws (e.g. OptimisticLockException), S3 is still untouched.
        var result = trustOnboardingSubmissionRepository.saveAndFlush(trustOnboardingSubmission);

        // Delete from S3+DB only after the flush succeeds.
        if (doiDocumentIdToDelete != null) {
            partnerDocumentService.deletePartnerDocument(doiDocumentIdToDelete);
        }

        return toTrustOnboardingsSubmissionDto(result);
    }

    /**
     * Returns true if any field that is reflected in the Declaration of Intent has changed.
     * Affected fields: UID, organisation name, address, DIDs (proof of possessions), signing rule, signatories.
     */
    private boolean hasDoiRelevantChanges(
        TrustOnboardingSubmission current,
        TrustOnboardingSubmissionRequestDto dto,
        String newUid
    ) {
        return (
            !Objects.equals(current.getUid(), newUid) ||
            !Objects.equals(current.getEntityName(), dto.entityName()) ||
            !Objects.equals(AddressMapper.toAddressDto(current.getEntityAddress()), dto.entityAddress()) ||
            !proofOfPossessionValidator.isDidSelectionEqual(current.getProofOfPossessions(), dto.dids()) ||
            !Objects.equals(current.getSigningRule(), toSigningRule(dto.signingRule())) ||
            !Objects.equals(current.getSignatories(), toSignatories(dto.signatories()))
        );
    }

    @Transactional
    public TrustOnboardingSubmissionDto submitProofOfPossessions(UUID partnerId, List<String> rawProofOfPossessions) {
        var trustOnboardingSubmission =
            trustOnboardingSubmissionDomainService.getUnsubmittedTrustOnboardingSubmissionByPartner(partnerId);

        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(
            rawProofOfPossessions,
            trustOnboardingSubmission.getProofOfPossessions()
        );

        if (errors.hasErrors()) {
            throw new ValidationException("Provided proof of possessions are not valid", errors);
        }

        var validPops = trustOnboardingSubmission
            .getProofOfPossessions()
            .stream()
            .map(ProofOfPossession::toValid)
            .toList();

        trustOnboardingSubmission.update(
            trustOnboardingSubmission.getEntityName(),
            trustOnboardingSubmission.getEntityAddress(),
            trustOnboardingSubmission.getEntityEmail(),
            trustOnboardingSubmission.getContactPerson(),
            trustOnboardingSubmission.getUid(),
            validPops,
            trustOnboardingSubmission.getRequestedPartnerType(),
            trustOnboardingSubmission.getSigningRule(),
            trustOnboardingSubmission.getSignatories(),
            trustOnboardingSubmission.getIsRegisteredInCommercialRegister()
        );
        return toTrustOnboardingsSubmissionDto(trustOnboardingSubmission);
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
    public void submit(UUID trustOnboardingSubmissionId, TrustOnboardingSubmitRequestDto requestDto) {
        var trustOnboardingSubmission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );

        if (!trustOnboardingSubmission.getVersion().equals(requestDto.getVersion())) {
            throw new OptimisticLockException(
                "Version mismatch " + trustOnboardingSubmission.getVersion() + " != " + requestDto.getVersion()
            );
        }

        var businessPartnerType = businessPartnerService.getBusinessPartnerType(
            trustOnboardingSubmission.getPartnerId()
        );

        Errors errors = validator.validate(trustOnboardingSubmission, businessPartnerType);

        if (errors.hasErrors()) {
            throw new ValidationException("Submission has missing/invalid fields.", errors);
        }

        trustOnboardingSubmission.markAsSubmitted();
        trustOnboardingSubmission = trustOnboardingSubmissionRepository.saveAndFlush(trustOnboardingSubmission);
        var doiS3Keys = partnerDocumentService.getAllStorageKeysByTrustOnboardingSubmissionId(
            trustOnboardingSubmission.getId()
        );
        auditPublisher.trustOnboardingSubmitted(
            trustOnboardingSubmission.getId().toString(),
            String.valueOf(trustOnboardingSubmission.getVersion()),
            trustOnboardingSubmission.getPartnerId().toString(),
            AuditMapper.toAuditJson(trustOnboardingSubmission),
            doiS3Keys
        );
        domainEventPublisher.publishTiTrustOnboardingSubmissionAcceptedEvent(
            EventMapper.mapToTiTrustOnboardingSubmissionAcceptedEvent(
                trustOnboardingSubmission.getId(),
                trustOnboardingSubmission.getPartnerId()
            )
        );
        var partnerId = trustOnboardingSubmission.getPartnerId();
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
}
