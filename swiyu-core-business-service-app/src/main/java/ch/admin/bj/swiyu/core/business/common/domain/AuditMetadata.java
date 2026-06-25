package ch.admin.bj.swiyu.core.business.common.domain;

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
public class AuditMetadata {

    @LastModifiedDate
    @NotNull
    private Instant lastModifiedAt;

    @LastModifiedBy
    @NotNull
    private String lastModifiedBy;

    @CreatedDate
    @NotNull
    private Instant createdAt;

    @CreatedBy
    @NotNull
    private String createdBy;
}
