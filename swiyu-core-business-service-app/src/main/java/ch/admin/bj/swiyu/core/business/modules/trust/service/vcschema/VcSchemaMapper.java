package ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema;

import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmissionStatus;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VcSchemaMapper {

    public static VcSchemaSubmissionDto toVcSchemaSubmissionDto(VcSchemaSubmission source) {
        return VcSchemaSubmissionDto.builder()
            .id(source.getId())
            .version(source.getVersion())
            .status(toVcSchemaSubmissionStatusDto(source.getStatus()))
            .file(source.getFile())
            .partnerId(source.getPartnerId())
            .updatedAt(source.getAuditMetadata().getLastModifiedAt())
            .createdAt(source.getAuditMetadata().getCreatedAt())
            .failureReason(Optional.ofNullable(source.getFailureReason()).orElse(""))
            .build();
    }

    public static VcSchemaSubmissionStatusDto toVcSchemaSubmissionStatusDto(VcSchemaSubmissionStatus source) {
        return switch (source) {
            case SUCCEEDED -> VcSchemaSubmissionStatusDto.SUCCEEDED;
            case FAILED -> VcSchemaSubmissionStatusDto.FAILED;
            case ACCEPTED -> VcSchemaSubmissionStatusDto.ACCEPTED;
        };
    }
}
