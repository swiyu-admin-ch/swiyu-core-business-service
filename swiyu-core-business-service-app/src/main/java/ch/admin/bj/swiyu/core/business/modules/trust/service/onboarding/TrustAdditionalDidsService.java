package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ValidationException;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionCreateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionInternalDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionResponseDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionUpdateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustAdditionalDidsSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsRejectReason;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmissionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.EventMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustAdditionalDidsMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation.ProofOfPossessionValidator;
import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.trust.registry.client.api.TrustStatementApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.SimpleErrors;

@Slf4j
@Service
@AllArgsConstructor
public class TrustAdditionalDidsService {

    private final TrustAdditionalDidsSubmissionRepository repository;
    private final TrustAdditionalDidsSubmissionLimitProperties limitProperties;
    private final TrustStatementApi trustStatementApi;
    private final IdentifierEntryService identifierEntryService;
    private final ProofOfPossessionValidator proofOfPossessionValidator;
    private final DomainEventPublisher domainEventPublisher;

    private static final String SUBMISSION_WITH_ID_NOT_FOUND = "TrustAdditionalDidsSubmission with id '%s' not found.";

    @Transactional
    public TrustAdditionalDidsSubmissionResponseDto createSubmission(
        UUID partnerId,
        TrustAdditionalDidsSubmissionCreateRequestDto dto
    ) {
        validatePermissionDid(partnerId, dto.permissionDid());
        validateDidsToAdd(partnerId, dto.didsToAdd());

        var sharedNonce = UUID.randomUUID().toString();
        var permissionDidPoP = new ProofOfPossession(dto.permissionDid(), sharedNonce);
        var didsToAddPoPs = dto
            .didsToAdd()
            .stream()
            .map(did -> new ProofOfPossession(did, sharedNonce))
            .toList();

        var submission = repository.save(new TrustAdditionalDidsSubmission(partnerId, permissionDidPoP, didsToAddPoPs));
        return TrustAdditionalDidsMapper.toTrustAdditionalDidsSubmissionResponseDto(submission);
    }

    @Transactional(readOnly = true)
    public TrustAdditionalDidsSubmissionResponseDto getSubmission(UUID submissionId, UUID partnerId) {
        return TrustAdditionalDidsMapper.toTrustAdditionalDidsSubmissionResponseDto(
            fetchByIdAndPartnerId(submissionId, partnerId)
        );
    }

    @Transactional(noRollbackFor = ValidationException.class)
    public TrustAdditionalDidsSubmissionResponseDto submitWithProofsOfPossession(
        UUID submissionId,
        UUID partnerId,
        TrustAdditionalDidsSubmissionUpdateRequestDto dto
    ) {
        var submission = fetchByIdAndPartnerId(submissionId, partnerId);

        if (submission.getStatus() != TrustAdditionalDidsSubmissionStatus.UNSUBMITTED) {
            var errors = new SimpleErrors(submission);
            errors.reject("EDITING_BLOCKED", "Submission is not in UNSUBMITTED state");
            throw new ValidationException("Submission is not in UNSUBMITTED state", errors);
        }

        var allRequiredPops = new ArrayList<ProofOfPossession>();
        allRequiredPops.add(submission.getPermissionDid());
        allRequiredPops.addAll(submission.getDidsToAdd());

        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(
            dto.proofOfPossessions(),
            allRequiredPops
        );

        if (errors.hasErrors()) {
            submission.refreshNonces();
            repository.save(submission);
            throw new ValidationException("Provided proof of possessions are not valid", errors);
        }

        submission.markAsValidatedAndSubmitted();

        log.info("TrustAdditionalDidsSubmission submitted, submissionId={}, partnerId={}", submissionId, partnerId);
        domainEventPublisher.publishTiTrustAddDidSubmissionSubmittedEvent(
            EventMapper.mapToTiTrustAddDidSubmissionSubmittedEvent(submissionId)
        );

        return TrustAdditionalDidsMapper.toTrustAdditionalDidsSubmissionResponseDto(repository.save(submission));
    }

    @Transactional
    public void markAsSucceeded(UUID submissionId) {
        var submission = getSubmissionById(submissionId);
        submission.markAsPublished();
        repository.save(submission);
        log.info("TrustAdditionalDidsSubmission marked as succeeded, submissionId={}", submissionId);
    }

    @Transactional
    public void markAsRejected(UUID submissionId, RejectReason rejectReason) {
        var submission = getSubmissionById(submissionId);
        submission.markAsFailed(toTrustAdditionalDidsRejectReason(rejectReason));
        repository.save(submission);
        log.info(
            "TrustAdditionalDidsSubmission marked as rejected, submissionId={}, reason={}",
            submissionId,
            rejectReason
        );
    }

    @Transactional(readOnly = true)
    public TrustAdditionalDidsSubmissionInternalDto getSubmissionInternal(UUID submissionId) {
        var submission = repository
            .findById(submissionId)
            .orElseThrow(() ->
                new ResourceNotFoundException(String.format(SUBMISSION_WITH_ID_NOT_FOUND, submissionId))
            );
        return TrustAdditionalDidsMapper.toTrustAdditionalDidsSubmissionInternalDto(submission);
    }

    @Transactional
    public void trustAdditionalDidsSubmissionCheckForUnsubmittedTimeout() {
        // To assert that the lock is held (prevents misconfiguration errors)
        LockAssert.assertLocked();

        var maxAgeTimestamp = Instant.now().minus(limitProperties.maxAgeInUnsubmitted());
        log.debug("Checking for TrustAdditionalDidsSubmissions expiry with maxAgeTimestamp={}", maxAgeTimestamp);
        var editedRows = repository.updateStatusToTimeout(maxAgeTimestamp);
        log.info("{} TrustAdditionalDidsSubmissions did expire.", editedRows);
    }

    private TrustAdditionalDidsSubmission getSubmissionById(UUID submissionId) {
        return repository
            .findById(submissionId)
            .orElseThrow(() ->
                new ResourceNotFoundException(String.format(SUBMISSION_WITH_ID_NOT_FOUND, submissionId))
            );
    }

    private void validatePermissionDid(UUID partnerId, String permissionDid) {
        if (!identifierEntryService.belongsDidToBusinessPartner(partnerId, permissionDid)) {
            var errors = new SimpleErrors(permissionDid);
            errors.reject("INVALID_DID", "permissionDid does not belong to the business partner");
            throw new ValidationException("permissionDid validation failed", errors);
        }

        var identityStatements = trustStatementApi.getIdentityTrustStatementsForDid(permissionDid, true);
        if (identityStatements == null || identityStatements.isEmpty()) {
            var errors = new SimpleErrors(permissionDid);
            errors.reject("MISSING_TRUST_STATEMENT", "permissionDid has no valid trust statement");
            throw new ValidationException("permissionDid trust statement validation failed", errors);
        }
    }

    private void validateDidsToAdd(UUID partnerId, List<String> didsToAdd) {
        var invalidDids = didsToAdd
            .stream()
            .filter(did -> !identifierEntryService.belongsDidToBusinessPartner(partnerId, did))
            .toList();

        if (!invalidDids.isEmpty()) {
            var errors = new SimpleErrors(didsToAdd);
            invalidDids.forEach(did ->
                errors.reject("INVALID_DID", "DID does not belong to the business partner: " + did)
            );
            throw new ValidationException("didsToAdd validation failed", errors);
        }
    }

    private static TrustAdditionalDidsRejectReason toTrustAdditionalDidsRejectReason(RejectReason source) {
        if (source == null) {
            return TrustAdditionalDidsRejectReason.UNKNOWN;
        }
        try {
            return TrustAdditionalDidsRejectReason.valueOf(source.name());
        } catch (IllegalArgumentException _) {
            log.error("Unknown reject reason: {}. Mapping it to UNKNOWN.", source);
            return TrustAdditionalDidsRejectReason.UNKNOWN;
        }
    }

    private TrustAdditionalDidsSubmission fetchByIdAndPartnerId(UUID submissionId, UUID partnerId) {
        return repository
            .findByIdAndPartnerId(submissionId, partnerId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    String.format(
                        "TrustAdditionalDidsSubmission with id '%s' not found for partner '%s'.",
                        submissionId,
                        partnerId
                    )
                )
            );
    }
}
