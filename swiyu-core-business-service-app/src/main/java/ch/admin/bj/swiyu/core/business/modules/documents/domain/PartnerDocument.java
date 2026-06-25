package ch.admin.bj.swiyu.core.business.modules.documents.domain;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import ch.admin.bj.swiyu.core.business.common.utils.MediaTypeConverter;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.http.MediaType;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
public class PartnerDocument {

    @Embedded
    @Valid
    @NotNull
    private final AuditMetadata auditMetadata = new AuditMetadata();

    Instant submittedAt;

    @NotNull
    @Convert(converter = MediaTypeConverter.class)
    MediaType mediaType;

    @Id
    private UUID id;

    @NotNull
    private UUID partnerId;

    private UUID trustOnboardingSubmissionId;

    private String virusScanId;

    @Version
    @NotNull
    private Long version;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PartnerDocumentType type;

    @NotEmpty
    private String fileName;

    @NotEmpty
    private String storageObjectKey;

    private PartnerDocument(
        UUID id,
        UUID partnerId,
        PartnerDocumentType type,
        String fileName,
        MediaType mediaType,
        String storageObjectKey,
        UUID trustOnboardingSubmissionId,
        String virusScanId,
        Instant submittedAt
    ) {
        this.id = id;
        this.submittedAt = submittedAt;
        this.mediaType = mediaType;
        this.partnerId = partnerId;
        this.trustOnboardingSubmissionId = trustOnboardingSubmissionId;
        this.virusScanId = virusScanId;
        this.type = type;
        this.fileName = fileName;
        this.storageObjectKey = storageObjectKey;
    }

    public static PartnerDocument createTrustOnboardingSubissionPartnerDocument(
        @NotNull UUID id,
        @NotNull UUID partnerId,
        @NotNull PartnerDocumentType type,
        @NotEmpty String fileName,
        @NotNull MediaType mediaType,
        @NotEmpty String storageObjectKey,
        @NotNull UUID trustOnboardingSubmissionId,
        @NotEmpty String virusScanId,
        @NotNull Instant submittedAt
    ) {
        if (
            !EnumSet.of(
                PartnerDocumentType.TRUST_ONBOARDING_OTHER,
                PartnerDocumentType.TRUST_ONBOARDING_DECLARATION_OF_INTENT
            ).contains(type)
        ) {
            throw new IllegalArgumentException("Invalid partnerDocumentType");
        }
        return new PartnerDocument(
            id,
            partnerId,
            type,
            fileName,
            mediaType,
            storageObjectKey,
            trustOnboardingSubmissionId,
            virusScanId,
            submittedAt
        );
    }
}
