package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

public interface TrustOnboardingSubmissionRepository
    extends JpaRepository<TrustOnboardingSubmission, UUID>, QuerydslPredicateExecutor<TrustOnboardingSubmission>
{
    TrustOnboardingSubmission findByPartnerIdAndStatusIn(
        UUID partnerId,
        List<TrustOnboardingSubmissionStatus> statuses
    );

    @Modifying
    @Query(
        "UPDATE TrustOnboardingSubmission e SET e.status = 'UNSUBMITTED_TIMEOUT' WHERE e.status = 'UNSUBMITTED' AND e.initiatedAt <= :maxAgeTimestamp"
    )
    int updateStatusToTimeout(@Param("maxAgeTimestamp") Instant maxAgeTimestamp);

    void deleteByPartnerId(UUID partnerId);

    List<TrustOnboardingSubmission> findAllByPartnerIdOrderByInitiatedAtAsc(@NotNull UUID partnerId);
}
