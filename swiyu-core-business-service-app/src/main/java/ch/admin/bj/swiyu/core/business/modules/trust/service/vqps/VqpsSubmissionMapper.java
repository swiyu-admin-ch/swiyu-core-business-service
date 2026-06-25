package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps.*;
import jakarta.validation.Valid;
import java.util.HashMap;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VqpsSubmissionMapper {

    public static VqpsSubmissionB2BDto toVqpsSubmissionB2BDto(VqpsSubmission source) {
        return new VqpsSubmissionB2BDto(
            source.getId(),
            source.getPartnerId(),
            source.getVersion(),
            toStatusDto(source.getStatus()),
            toVqpsPublicationResultDto(source.getPublicationResult()),
            toVqpsPublicationFailureReasonDto(source.getPublicationFailureReason()),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt()
        );
    }

    public static VqpsSubmissionInternalDto toVqpsSubmissionInternalDto(VqpsSubmission source) {
        return new VqpsSubmissionInternalDto(
            source.getId(),
            source.getPartnerId(),
            source.getVersion(),
            toStatusDto(source.getStatus()),
            source.getSub(),
            new HashMap<>(source.getPurposeName()),
            new HashMap<>(source.getPurposeDescription()),
            source.getScope(),
            source.getQuery(),
            toVqpsPublicationFailureReasonDto(source.getPublicationFailureReason()),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt()
        );
    }

    public static VqpsPublicationFailureReason toVqpsPublicationFailureReason(VqpsPublicationFailureReasonDto source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case VqpsPublicationFailureReasonDto.UNKNOWN -> VqpsPublicationFailureReason.UNKNOWN;
        };
    }

    public static VqpsPublicationFailureReasonDto toVqpsPublicationFailureReasonDto(
        ch.admin.bj.swiyu.messagetype.ti.VqpsPublicationFailureReason source
    ) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case ch.admin.bj.swiyu.messagetype.ti.VqpsPublicationFailureReason.UNKNOWN -> VqpsPublicationFailureReasonDto.UNKNOWN;
        };
    }

    public static VqpsPublicationFailureReasonDto toVqpsPublicationFailureReasonDto(
        VqpsPublicationFailureReason source
    ) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case VqpsPublicationFailureReason.UNKNOWN -> VqpsPublicationFailureReasonDto.UNKNOWN;
        };
    }

    public static VqpsSubmissionStatusDto toStatusDto(VqpsSubmissionStatus source) {
        return switch (source) {
            case ACCEPTED -> VqpsSubmissionStatusDto.ACCEPTED;
            case PUBLICATION_SUCCEEDED -> VqpsSubmissionStatusDto.PUBLICATION_SUCCEEDED;
            case PUBLICATION_FAILED -> VqpsSubmissionStatusDto.PUBLICATION_FAILED;
        };
    }

    private static VqpsPublicationResultDto toVqpsPublicationResultDto(@Valid VqpsPublicationResult source) {
        if (source == null) {
            return null;
        }
        return new VqpsPublicationResultDto(source.getJti().toString(), source.getJwt(), source.getExpiresAt());
    }
}
