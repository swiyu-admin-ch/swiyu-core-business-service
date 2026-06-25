package ch.admin.bj.swiyu.registry.identifier.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Embeddable to provide auto tracking of audit data for an entity in the DB.
 */
@Embeddable
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
class AuditMetadata {

    @Column(nullable = false)
    @LastModifiedDate
    @NotNull
    private Instant lastModifiedAt;

    @Column(nullable = false)
    @LastModifiedBy
    @NotNull
    private String lastModifiedBy;

    @Column(nullable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(nullable = false)
    @CreatedBy
    @NotNull
    private String createdBy;
}
