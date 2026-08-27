package ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class ProtectedVerificationSubmission {

    @Embedded
    @Valid
    @NotNull
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Id
    private UUID id;

    @NotNull
    private UUID sbnId; // unique identifier for organizations authorized by ZAS

    @NotNull
    private UUID partnerId;

    @NotBlank
    private String entityName;

    private String uid;

    @Embedded
    @Valid
    @AttributeOverride(name = "firstName", column = @Column(name = "contact_first_name"))
    @AttributeOverride(name = "lastName", column = @Column(name = "contact_last_name"))
    @AttributeOverride(name = "email", column = @Column(name = "contact_email"))
    @AttributeOverride(name = "phone", column = @Column(name = "contact_phone"))
    @AttributeOverride(name = "correspondingLanguage", column = @Column(name = "contact_corresponding_language"))
    private Contact contactPerson;

    @NotBlank
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProtectedVerificationCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProtectedVerificationSubmissionStatus status;

    private String rejectReason;

    @NotNull
    private Instant submittedAt;

    @Version
    @NotNull
    private Long version;

    protected ProtectedVerificationSubmission() {
        // JPA
    }

    public ProtectedVerificationSubmission(
        UUID partnerId,
        UUID sbnId,
        String entityName,
        String uid,
        Contact contactPerson,
        String reason,
        ProtectedVerificationCategory category
    ) {
        this(UUID.randomUUID(), partnerId, sbnId, entityName, uid, contactPerson, reason, category);
    }

    /**
     * Accepts an explicit id, so demo data can reference a submission's id from the matching
     * ProtectedVerificationRequestTask it seeds on the trust-management-scs side (there is no other way to know
     * it in advance, since it's otherwise only communicated via the TiProtectedVerificationSubmissionAcceptedEvent
     * published after construction).
     */
    public ProtectedVerificationSubmission(
        UUID id,
        UUID partnerId,
        UUID sbnId,
        String entityName,
        String uid,
        Contact contactPerson,
        String reason,
        ProtectedVerificationCategory category
    ) {
        this.id = id;
        this.partnerId = partnerId;
        this.sbnId = sbnId;
        this.entityName = entityName;
        this.uid = uid;
        this.contactPerson = contactPerson;
        this.reason = reason;
        this.category = category;
        this.status = ProtectedVerificationSubmissionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void markAsApproved() {
        this.status = ProtectedVerificationSubmissionStatus.APPROVED;
    }

    public void markAsRejected(String rejectReason) {
        this.status = ProtectedVerificationSubmissionStatus.REJECTED;
        this.rejectReason = rejectReason;
    }
}
