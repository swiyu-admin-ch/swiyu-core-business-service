package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerType;
import static ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentMapper.toTrustOnboardingSubmissionDocumentListItemDto;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustOnboardingMapper.*;

import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.exceptions.ValidationException;
import ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentListItemDto;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerTrustStatusDto;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.EventMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustOnboardingMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
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
import org.springframework.validation.Errors;

@Slf4j
@Service
@AllArgsConstructor
public class TrustOnboardingService {

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
                new DeclarationOfIntent(document.id().toString(), doiValidationFileReportAsJson)
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
                TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
            )
        );
        if (inProgressEntry != null) {
            return toTrustOnboardingSubmissionDto(inProgressEntry);
        }

        String uid = null;
        if (dto.getRegistryIds() != null && dto.getRegistryIds().containsKey("UID")) {
            uid = dto.getRegistryIds().get("UID");
        }

        TrustOnboardingSubmission trustOnboardingSubmission = trustOnboardingSubmissionRepository.save(
            new TrustOnboardingSubmission(
                dto.partnerId(),
                dto.getEntityName(),
                AddressMapper.toAddressEntity(dto.entityAddress()),
                dto.getEntityEmail(),
                toContactEntity(dto.getContactPerson()),
                toLanguageEntity(dto.correspondingLanguage()),
                uid,
                true,
                TrustOnboardingMapper.toProofOfPossession(dto.dids()),
                toBusinessPartnerType(dto.requestedPartnerType()),
                toSigningRule(dto.signingRule()),
                toSignatories(dto.signatories())
            )
        );
        return toTrustOnboardingSubmissionDto(trustOnboardingSubmission);
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

        String uid = null;
        if (dto.getRegistryIds() != null && dto.getRegistryIds().containsKey("UID")) {
            uid = dto.getRegistryIds().get("UID");
        }

        // Determine before update whether DOI-relevant fields are changing
        boolean discardDoi = hasDoiRelevantChanges(trustOnboardingSubmission, dto, uid);

        var popList = trustOnboardingSubmission.getProofOfPossessions();
        if (!proofOfPossessionValidator.isDidSelectionEqual(popList, dto.dids())) {
            popList = TrustOnboardingMapper.toProofOfPossession(dto.dids());
        }

        trustOnboardingSubmission.update(
            dto.getEntityName(),
            AddressMapper.toAddressEntity(dto.entityAddress()),
            dto.getEntityEmail(),
            toContactEntity(dto.getContactPerson()),
            toLanguageEntity(dto.correspondingLanguage()),
            uid,
            popList,
            toBusinessPartnerType(dto.requestedPartnerType()),
            toSigningRule(dto.signingRule()),
            toSignatories(dto.signatories()),
            Boolean.TRUE.equals(dto.isRegisteredInCommercialRegister())
        );

        // If DOI-relevant fields changed, the signed DOI is no longer valid and must be deleted.
        // Capture the ID before nulling the reference — we need it for the S3 delete after flush.
        UUID doiDocumentIdToDelete = null;
        if (discardDoi) {
            var doi = trustOnboardingSubmission.getDeclarationOfIntent();
            if (doi != null) {
                doiDocumentIdToDelete = UUID.fromString(doi.fullySignedDocumentId());
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
            !Objects.equals(current.getEntityName(), dto.getEntityName()) ||
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
            trustOnboardingSubmission.getCorrespondingLanguage(),
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
        aggregateTrustVerificationStatus(trustOnboardingSubmission.getPartnerId()); // NOSONAR invoking transactional method is fine here
        domainEventPublisher.publishTiTrustOnboardingSubmissionAcceptedEvent(
            EventMapper.mapToTiTrustOnboardingSubmissionAcceptedEvent(
                trustOnboardingSubmission.getId(),
                trustOnboardingSubmission.getPartnerId()
            )
        );
    }

    @Transactional
    public void markAsRejected(UUID trustOnboardingSubmissionId, String rejectReason) {
        var trustOnboardingSubmission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        trustOnboardingSubmission.markAsRejected(toTrustOnboardingRejectReason(rejectReason));
        aggregateTrustVerificationStatus(trustOnboardingSubmission.getPartnerId()); // NOSONAR invoking transactional method is fine here
    }

    @Transactional
    public void markAsInformationRequested(UUID trustOnboardingSubmissionId, String declineReason, String partnerNote) {
        var trustOnboardingSubmission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        trustOnboardingSubmission.markAsInformationRequested(
            toTrustOnboardingDeclineReason(declineReason),
            partnerNote
        );
        aggregateTrustVerificationStatus(trustOnboardingSubmission.getPartnerId()); // NOSONAR invoking transactional method is fine here
    }

    @Transactional
    public void markAsSucceeded(UUID trustOnboardingSubmissionId) {
        var trustOnboardingSubmission = trustOnboardingSubmissionDomainService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        trustOnboardingSubmission.markAsSucceeded();

        updateBusinessPartnerWithSubmissionDetails(trustOnboardingSubmission);

        aggregateTrustVerificationStatus(trustOnboardingSubmission.getPartnerId()); // NOSONAR invoking transactional method is fine here
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

    private TrustOnboardingSubmissionDto toTrustOnboardingsSubmissionDto(
        TrustOnboardingSubmission trustOnboardingSubmission
    ) {
        return TrustOnboardingMapper.toTrustOnboardingSubmissionDto(trustOnboardingSubmission);
    }

    /**
     * Calculates the TrustVerificationStatus of a BusinessPartner according to the TrustOnboardingSubmissions belonging to it
     *
     * @param partnerId The BusinessPartner to update
     */
    @Transactional
    public void aggregateTrustVerificationStatus(@NotNull UUID partnerId) {
        var trustOnboardingSubmissions = trustOnboardingSubmissionRepository.findAllByPartnerIdOrderByInitiatedAtAsc(
            partnerId
        );
        BusinessPartnerTrustStatusDto aggregatedState = BusinessPartnerTrustStatusDto.NOT_VERIFIED;
        Instant maxDateForTrustVerificationStatus = null;

        var hasPreviouslySucceeded = false;
        for (var trustOnboardingSubmission : trustOnboardingSubmissions) {
            switch (trustOnboardingSubmission.getStatus()) {
                case SUCCEEDED: {
                    aggregatedState = BusinessPartnerTrustStatusDto.VERIFIED;
                    hasPreviouslySucceeded = true;
                    break;
                }
                case REJECTED: {
                    aggregatedState = BusinessPartnerTrustStatusDto.NOT_VERIFIED;
                    hasPreviouslySucceeded = false;
                    break;
                }
                case UNSUBMITTED: {
                    // mark the time limit to submit the trust onboarding submission
                    maxDateForTrustVerificationStatus = trustOnboardingSubmission
                        .getInitiatedAt()
                        .plus(limitProperties.maxAgeInUnsubmitted());
                    if (hasPreviouslySucceeded) {
                        aggregatedState = BusinessPartnerTrustStatusDto.RE_VERIFICATION_STARTED;
                    } else {
                        aggregatedState = BusinessPartnerTrustStatusDto.VERIFICATION_STARTED;
                    }
                    break;
                }
                case UNSUBMITTED_TIMEOUT: {
                    if (hasPreviouslySucceeded) {
                        aggregatedState = BusinessPartnerTrustStatusDto.VERIFIED;
                    } else {
                        aggregatedState = BusinessPartnerTrustStatusDto.NOT_VERIFIED;
                    }
                    break;
                }
                case INFORMATION_REQUESTED: {
                    aggregatedState = BusinessPartnerTrustStatusDto.INFORMATION_REQUESTED;
                    break;
                }
                case SUBMITTED: {
                    if (hasPreviouslySucceeded) {
                        aggregatedState = BusinessPartnerTrustStatusDto.RE_VERIFICATION_IN_PROGRESS;
                    } else {
                        aggregatedState = BusinessPartnerTrustStatusDto.VERIFICATION_IN_PROGRESS;
                    }
                }
            }
        }
        businessPartnerService.changeTrustVerificationStatus(
            partnerId,
            aggregatedState,
            maxDateForTrustVerificationStatus
        );
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
