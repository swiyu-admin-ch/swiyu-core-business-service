package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import static ch.admin.bj.swiyu.core.business.modules.trust.service.vqps.VqpsSubmissionMapper.toVqpsPublicationFailureReason;

import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps.VqpsPublicationResult;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps.VqpsSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps.VqpsSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.EventMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class VqpsSubmissionService {

    private final VqpsSubmissionRepository vqpsSubmissionRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final DcqlQueryValidator dcqlQueryValidator;

    @Transactional
    public VqpsSubmissionB2BDto createVqpsSubmission(VqpsSubmissionCreateRequestDto request, UUID partnerId) {
        log.info("Creating VqpsSubmission for partnerId {} with purposeName {}", partnerId, request.purposeName());
        dcqlQueryValidator.validateDcqlQuery(request.query());
        var submission = vqpsSubmissionRepository.save(
            new VqpsSubmission(
                partnerId,
                request.sub(),
                request.purposeName(),
                request.purposeDescription(),
                request.scope(),
                request.query()
            )
        );
        domainEventPublisher.publishVqpsSubmissionAcceptedEvent(
            EventMapper.mapToTiVqpsSubmissionAcceptedEvent(submission.getId())
        );
        return VqpsSubmissionMapper.toVqpsSubmissionB2BDto(submission);
    }

    @Transactional(readOnly = true)
    public VqpsSubmissionB2BDto getVqpsSubmissionB2B(UUID submissionId) {
        return vqpsSubmissionRepository
            .findById(submissionId)
            .map(VqpsSubmissionMapper::toVqpsSubmissionB2BDto)
            .orElseThrow(handleSubmissionNotFound(submissionId));
    }

    @Transactional(readOnly = true)
    public VqpsSubmissionInternalDto getVqpsSubmissionInternal(UUID submissionId) {
        return vqpsSubmissionRepository
            .findById(submissionId)
            .map(VqpsSubmissionMapper::toVqpsSubmissionInternalDto)
            .orElseThrow(handleSubmissionNotFound(submissionId));
    }

    @Transactional(readOnly = true)
    public Page<VqpsSubmissionB2BDto> getVqpsSubmissionsB2B(UUID partnerId, Pageable pageable) {
        return vqpsSubmissionRepository
            .findAllByPartnerId(
                partnerId,
                PageableUtils.toDbPageableFromUserPageable(VqpsSubmissionB2BDto.class, VqpsSubmission.class, pageable)
            )
            .map(VqpsSubmissionMapper::toVqpsSubmissionB2BDto);
    }

    @Transactional
    public void markAsPublicationSucceeded(UUID submissionId, String vpqsJwt) {
        log.info("Marking VqpsSubmission with id {} as succeeded", submissionId);
        var submission = vqpsSubmissionRepository
            .findById(submissionId)
            .orElseThrow(handleSubmissionNotFound(submissionId));
        var result = readPublicationResultFromJwt(vpqsJwt);
        submission.markAsSucceeded(result);
        vqpsSubmissionRepository.save(submission);
    }

    /**
     * Extracts "jti" and "exp" claim from jwt and returns VqpsPublicationResult containing this info.
     */
    private static @NonNull VqpsPublicationResult readPublicationResultFromJwt(String vpqsJwt) {
        SignedJWT signedJWT;
        try {
            signedJWT = SignedJWT.parse(vpqsJwt);
        } catch (ParseException e) {
            throw new IllegalStateException(
                "Failed to read VQPS Publication result. The JWT of the Verification Query Public Statements that was issued by TMS could not be read.",
                e
            );
        }
        JWTClaimsSet claims;
        try {
            claims = signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new IllegalStateException(
                "Failed to read VQPS Publication result. The claims of the JWT of the Verification Query Public Statements that was issued by TMS could not be read.",
                e
            );
        }
        var expires = claims.getExpirationTime().toInstant();
        var jti = UUID.fromString(claims.getJWTID());
        return new VqpsPublicationResult(jti, vpqsJwt, expires);
    }

    @Transactional
    public void markAsPublicationFailed(UUID submissionId, VqpsPublicationFailureReasonDto reason) {
        log.info("Marking VqpsSubmission with id {} as failed with reason {}", submissionId, reason);
        var submission = vqpsSubmissionRepository
            .findById(submissionId)
            .orElseThrow(handleSubmissionNotFound(submissionId));
        submission.markAsFailed(toVqpsPublicationFailureReason(reason));
    }

    private static @NonNull Supplier<ResourceNotFoundException> handleSubmissionNotFound(UUID submissionId) {
        return () -> new ResourceNotFoundException("No VqpsSubmission found with id " + submissionId);
    }
}
