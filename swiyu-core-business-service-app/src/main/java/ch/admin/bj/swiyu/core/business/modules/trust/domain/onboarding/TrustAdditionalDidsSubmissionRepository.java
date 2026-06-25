package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrustAdditionalDidsSubmissionRepository extends JpaRepository<TrustAdditionalDidsSubmission, UUID> {
    Optional<TrustAdditionalDidsSubmission> findByIdAndPartnerId(UUID id, UUID partnerId);

    @Modifying
    @Query(
        "UPDATE TrustAdditionalDidsSubmission e SET e.status = 'UNSUBMITTED_TIMEOUT' WHERE e.status = 'UNSUBMITTED' AND e.auditMetadata.createdAt <= :maxAgeTimestamp"
    )
    int updateStatusToTimeout(@Param("maxAgeTimestamp") Instant maxAgeTimestamp);
}
