package ch.admin.bj.swiyu.core.business.modules.trust.service.mapper;

import ch.admin.bj.swiyu.core.business.modules.trust.api.ProofOfPossessionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProofOfPossessionStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionInternalDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionResponseDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossessionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmissionStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TrustAdditionalDidsMapper {

    public static TrustAdditionalDidsSubmissionResponseDto toTrustAdditionalDidsSubmissionResponseDto(
        TrustAdditionalDidsSubmission source
    ) {
        return new TrustAdditionalDidsSubmissionResponseDto(
            source.getId(),
            UUID.fromString(source.getPermissionDid().getNonce()),
            source.getPermissionDid().getDid(),
            source.getDidsToAdd().stream().map(ProofOfPossession::getDid).toList(),
            toTrustAdditionalDidsSubmissionStatusDto(source.getStatus())
        );
    }

    public static TrustAdditionalDidsSubmissionInternalDto toTrustAdditionalDidsSubmissionInternalDto(
        TrustAdditionalDidsSubmission source
    ) {
        return new TrustAdditionalDidsSubmissionInternalDto(
            source.getId(),
            toTrustAdditionalDidsSubmissionStatusDto(source.getStatus()),
            toProofOfPossessionDto(source.getPermissionDid()),
            toProofOfPossessionDtos(source.getDidsToAdd()),
            truncateInstantToMicroseconds(source.getAuditMetadata().getLastModifiedAt())
        );
    }

    private static TrustAdditionalDidsSubmissionStatusDto toTrustAdditionalDidsSubmissionStatusDto(
        TrustAdditionalDidsSubmissionStatus status
    ) {
        return switch (status) {
            case UNSUBMITTED -> TrustAdditionalDidsSubmissionStatusDto.UNSUBMITTED;
            case UNSUBMITTED_TIMEOUT -> TrustAdditionalDidsSubmissionStatusDto.UNSUBMITTED_TIMEOUT;
            case SUBMITTED -> TrustAdditionalDidsSubmissionStatusDto.SUBMITTED;
            case SUCCEEDED -> TrustAdditionalDidsSubmissionStatusDto.SUCCEEDED;
            case REJECTED -> TrustAdditionalDidsSubmissionStatusDto.REJECTED;
        };
    }

    private static List<ProofOfPossessionDto> toProofOfPossessionDtos(List<ProofOfPossession> source) {
        return source.stream().map(TrustAdditionalDidsMapper::toProofOfPossessionDto).toList();
    }

    private static ProofOfPossessionDto toProofOfPossessionDto(ProofOfPossession source) {
        return new ProofOfPossessionDto(
            source.getDid(),
            source.getNonce(),
            toPopStatusDto(source.getStatus()),
            truncateInstantToMicroseconds(source.getVerifiedAt())
        );
    }

    private static ProofOfPossessionStatusDto toPopStatusDto(ProofOfPossessionStatus status) {
        return switch (status) {
            case VALID -> ProofOfPossessionStatusDto.VALID;
            case NOT_SUPPLIED -> ProofOfPossessionStatusDto.NOT_SUPPLIED;
        };
    }

    private static Instant truncateInstantToMicroseconds(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.truncatedTo(ChronoUnit.MICROS);
    }
}
