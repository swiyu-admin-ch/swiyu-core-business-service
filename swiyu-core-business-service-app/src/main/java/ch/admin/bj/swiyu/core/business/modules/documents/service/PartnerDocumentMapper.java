package ch.admin.bj.swiyu.core.business.modules.documents.service;

import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentListItemDto;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocument;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentType;
import jakarta.validation.constraints.NotNull;
import java.net.URL;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PartnerDocumentMapper {

    public static TrustOnboardingSubmissionDocumentListItemDto toTrustOnboardingSubmissionDocumentListItemDto(
        PartnerDocumentDto source,
        boolean canBeDeleted
    ) {
        return new TrustOnboardingSubmissionDocumentListItemDto(
            source.id(),
            source.createdAt(),
            source.lastModifiedAt(),
            source.trustOnboardingSubmissionId(),
            source.fileName(),
            source.mediaType(),
            source.type(),
            source.partnerId(),
            source.submittedAt(),
            canBeDeleted
        );
    }

    private static PartnerDocumentTypeDto toPartnerDocumentTypeDto(@NotNull PartnerDocumentType source) {
        return switch (source) {
            case TRUST_ONBOARDING_OTHER -> PartnerDocumentTypeDto.TRUST_ONBOARDING_OTHER;
            case TRUST_ONBOARDING_DECLARATION_OF_INTENT -> PartnerDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT;
        };
    }

    public static TrustOnboardingSubmissionDocumentDto toTrustOnboardingSubmissionDocumentDto(
        PartnerDocument source,
        URL presignedLink
    ) {
        return new TrustOnboardingSubmissionDocumentDto(
            source.getId(),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt(),
            source.getTrustOnboardingSubmissionId(),
            presignedLink,
            source.getVersion(),
            source.getFileName(),
            source.getMediaType().toString(),
            toPartnerDocumentTypeDto(source.getType()),
            source.getPartnerId(),
            source.getSubmittedAt()
        );
    }

    public static PartnerDocumentType toPartnerDocumentType(@NotNull PartnerDocumentTypeDto source) {
        return switch (source) {
            case TRUST_ONBOARDING_DECLARATION_OF_INTENT -> PartnerDocumentType.TRUST_ONBOARDING_DECLARATION_OF_INTENT;
            case TRUST_ONBOARDING_OTHER -> PartnerDocumentType.TRUST_ONBOARDING_OTHER;
        };
    }

    public static PartnerDocumentDto toPartnerDocumentDto(PartnerDocument source) {
        if (source == null) {
            return null;
        }
        return new PartnerDocumentDto(
            source.getId(),
            source.getAuditMetadata().getCreatedAt(),
            source.getAuditMetadata().getLastModifiedAt(),
            source.getTrustOnboardingSubmissionId(),
            source.getFileName(),
            source.getMediaType().toString(),
            toPartnerDocumentTypeDto(source.getType()),
            source.getPartnerId(),
            source.getSubmittedAt()
        );
    }
}
