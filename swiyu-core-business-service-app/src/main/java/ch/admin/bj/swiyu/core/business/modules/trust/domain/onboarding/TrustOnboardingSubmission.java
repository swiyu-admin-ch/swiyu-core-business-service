package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import ch.admin.bj.swiyu.core.business.common.domain.*;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class TrustOnboardingSubmission {

    @Embedded
    @Valid
    @NotNull
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Nullable
    private Instant submittedAt;

    @NotNull
    private Instant initiatedAt;

    @Getter
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Valid
    private List<ProofOfPossession> proofOfPossessions;

    @Getter
    @Id
    private UUID id;

    @NotNull
    private UUID partnerId;

    @Version
    @NotNull
    private Long version;

    @Getter
    @Embedded
    @AttributeOverride(name = "de", column = @Column(name = "entity_name_de"))
    @AttributeOverride(name = "fr", column = @Column(name = "entity_name_fr"))
    @AttributeOverride(name = "it", column = @Column(name = "entity_name_it"))
    @AttributeOverride(name = "en", column = @Column(name = "entity_name_en"))
    @AttributeOverride(name = "rm", column = @Column(name = "entity_name_rm"))
    @Valid
    private MultiLanguageText entityName;

    @Getter
    @Embedded
    @AttributeOverride(name = "street", column = @Column(name = "entity_street"))
    @AttributeOverride(name = "postalCode", column = @Column(name = "entity_postal_code"))
    @AttributeOverride(name = "city", column = @Column(name = "entity_city"))
    @AttributeOverride(name = "country", column = @Column(name = "entity_country"))
    @AttributeOverride(name = "region", column = @Column(name = "entity_region"))
    @Valid
    private Address entityAddress;

    @Getter
    private String entityEmail;

    @Getter
    @Embedded
    @AttributeOverride(name = "address.street", column = @Column(name = "contact_street"))
    @AttributeOverride(name = "address.postalCode", column = @Column(name = "contact_postal_code"))
    @AttributeOverride(name = "address.city", column = @Column(name = "contact_city"))
    @AttributeOverride(name = "address.country", column = @Column(name = "contact_country"))
    @AttributeOverride(name = "phone", column = @Column(name = "contact_phone"))
    @AttributeOverride(name = "email", column = @Column(name = "contact_email"))
    @AttributeOverride(name = "firstName", column = @Column(name = "contact_first_name"))
    @AttributeOverride(name = "lastName", column = @Column(name = "contact_last_name"))
    @Valid
    private Contact contactPerson;

    @Getter
    @Enumerated(EnumType.STRING)
    private SigningRule signingRule;

    @Getter
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Valid
    private List<Signatory> signatories;

    @Getter
    private String uid;

    @Getter
    private Boolean isRegisteredInCommercialRegister; // 'Handelsregistereintrag'

    @Getter
    private Language correspondingLanguage;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TrustOnboardingSubmissionStatus status;

    @Getter
    @Enumerated(EnumType.STRING)
    private TrustOnboardingRejectReason rejectReason;

    @Getter
    @Enumerated(EnumType.STRING)
    private TrustOnboardingDeclineReason declineReason;

    @Getter
    private String partnerNote;

    @Getter
    @Enumerated(EnumType.STRING)
    private BusinessPartnerType requestedPartnerType;

    @Getter
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Valid
    private DeclarationOfIntent declarationOfIntent;

    protected TrustOnboardingSubmission() {
        // JPA
    }

    @VisibleForTesting
    public TrustOnboardingSubmission(
        UUID partnerId,
        MultiLanguageText entityName,
        TrustOnboardingSubmissionStatus status
    ) {
        this.id = UUID.randomUUID();
        this.partnerId = partnerId;
        this.entityName = entityName;
        this.status = status;
        this.initiatedAt = Instant.now();
    }

    public TrustOnboardingSubmission( // NOSONAR
        UUID partnerId,
        MultiLanguageText entityName,
        Address entityAddress,
        String entityEmail,
        Contact contactPerson,
        Language correspondingLanguage,
        String uid,
        Boolean isRegisteredInCommercialRegister,
        List<ProofOfPossession> proofOfPossessions,
        BusinessPartnerType requestedPartnerType,
        SigningRule signingRule,
        List<Signatory> signatories
    ) {
        this(
            UUID.randomUUID(),
            partnerId,
            entityName,
            entityAddress,
            entityEmail,
            contactPerson,
            correspondingLanguage,
            uid,
            isRegisteredInCommercialRegister,
            proofOfPossessions,
            requestedPartnerType,
            signingRule,
            signatories,
            Instant.now()
        );
    }

    @VisibleForTesting
    public TrustOnboardingSubmission( // NOSONAR
        UUID id,
        UUID partnerId,
        MultiLanguageText entityName,
        Address entityAddress,
        String entityEmail,
        Contact contactPerson,
        Language correspondingLanguage,
        String uid,
        Boolean isRegisteredInCommercialRegister,
        List<ProofOfPossession> proofOfPossessions,
        BusinessPartnerType partnerType,
        SigningRule signingRule,
        List<Signatory> signatories,
        Instant initiatedAt
    ) {
        this.id = id;
        this.partnerId = partnerId;
        this.entityName = entityName;
        this.entityAddress = entityAddress;
        this.entityEmail = entityEmail;
        this.contactPerson = contactPerson;
        this.correspondingLanguage = correspondingLanguage;
        this.uid = uid;
        this.isRegisteredInCommercialRegister = isRegisteredInCommercialRegister;
        this.proofOfPossessions = proofOfPossessions;
        this.status = TrustOnboardingSubmissionStatus.UNSUBMITTED;
        this.initiatedAt = initiatedAt;
        this.requestedPartnerType = partnerType;
        this.signingRule = signingRule;
        this.signatories = signatories;
    }

    public void markAsSubmitted() {
        this.status = TrustOnboardingSubmissionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void markAsRejected(TrustOnboardingRejectReason rejectReason) {
        this.status = TrustOnboardingSubmissionStatus.REJECTED;
        this.rejectReason = rejectReason;
    }

    public void markAsSucceeded() {
        this.status = TrustOnboardingSubmissionStatus.SUCCEEDED;
    }

    @VisibleForTesting
    public void markAsExpired() {
        this.status = TrustOnboardingSubmissionStatus.UNSUBMITTED_TIMEOUT;
    }

    @VisibleForTesting
    public void setRequestedPartnerType(BusinessPartnerType requestedPartnerType) {
        this.requestedPartnerType = requestedPartnerType;
    }

    public void markAsInformationRequested(TrustOnboardingDeclineReason declineReason, String partnerNote) {
        this.status = TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED;
        this.declineReason = declineReason;
        this.partnerNote = partnerNote;
        this.proofOfPossessions = this.proofOfPossessions.stream().map(ProofOfPossession::toNotSupplied).toList();
    }

    public void updateDeclarationOfIntent(DeclarationOfIntent declarationOfIntent) {
        this.declarationOfIntent = declarationOfIntent;
    }

    // Only clear the DOI if the deleted document is actually the referenced one.
    // Deleting any other document on this submission must not wipe the DOI reference.
    public void removeDeclarationOfIntent(UUID documentId) {
        if (
            this.declarationOfIntent != null &&
            documentId.toString().equals(this.declarationOfIntent.fullySignedDocumentId())
        ) {
            this.declarationOfIntent = null;
        }
    }

    public void update( // NOSONAR
        MultiLanguageText entityName,
        Address entityAddress,
        String entityEmail,
        Contact contactPerson,
        Language correspondingLanguage,
        String uid,
        List<ProofOfPossession> proofOfPossession,
        BusinessPartnerType requestedPartnerType,
        SigningRule signingRule,
        List<Signatory> signatories,
        boolean isRegisteredInCommercialRegister
    ) {
        this.entityName = entityName;
        this.entityAddress = entityAddress;
        this.entityEmail = entityEmail;
        this.contactPerson = contactPerson;
        this.correspondingLanguage = correspondingLanguage;
        this.uid = uid;
        this.proofOfPossessions = proofOfPossession;
        this.requestedPartnerType = requestedPartnerType;
        this.signingRule = signingRule;
        this.signatories = signatories;
        this.isRegisteredInCommercialRegister = isRegisteredInCommercialRegister;
    }
}
