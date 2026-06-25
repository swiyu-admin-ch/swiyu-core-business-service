package ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps;

import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import ch.admin.bj.swiyu.core.business.common.i18n.ValidLocalizedMap;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class VqpsSubmission {

    @Id
    private UUID id;

    @NotNull
    private UUID partnerId;

    @Version
    @NotNull
    private Long version;

    @Embedded
    @Valid
    @NotNull
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @NotNull
    @Enumerated(EnumType.STRING)
    private VqpsSubmissionStatus status;

    @NotNull
    private String sub;

    @ValidLocalizedMap
    @NotNull
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, @NotBlank @Size(max = 40) String> purposeName;

    @ValidLocalizedMap
    @NotNull
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, @NotBlank @Size(max = 1000) String> purposeDescription;

    @NotNull
    private String scope;

    @NotNull
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode query;

    @Enumerated(EnumType.STRING)
    private VqpsPublicationFailureReason publicationFailureReason;

    @Embedded
    @Valid
    @AttributeOverride(name = "jti", column = @Column(name = "publication_result_jti"))
    @AttributeOverride(name = "jwt", column = @Column(name = "publication_result_jwt"))
    @AttributeOverride(name = "expiresAt", column = @Column(name = "publication_result_expires_at"))
    private VqpsPublicationResult publicationResult;

    protected VqpsSubmission() {
        // JPA
    }

    public VqpsSubmission(
        UUID partnerId,
        String sub,
        Map<String, String> purposeName,
        Map<String, String> purposeDescription,
        String scope,
        JsonNode query
    ) {
        this.id = UUID.randomUUID();
        this.partnerId = partnerId;
        this.status = VqpsSubmissionStatus.ACCEPTED;
        this.sub = sub;
        this.purposeName = purposeName;
        this.purposeDescription = purposeDescription;
        this.scope = scope;
        this.query = query;
    }

    public void markAsSucceeded(VqpsPublicationResult publicationResult) {
        this.status = VqpsSubmissionStatus.PUBLICATION_SUCCEEDED;
        this.publicationResult = publicationResult;
    }

    public void markAsFailed(VqpsPublicationFailureReason reason) {
        this.status = VqpsSubmissionStatus.PUBLICATION_FAILED;
        this.publicationFailureReason = reason;
    }
}
