package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import ch.admin.bj.swiyu.core.business.common.email.PendingReviewSubmission;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Submissions still waiting for review that were submitted before the cutoff.
     *
     * <p>No lower bound on {@code submittedAt}: a submission that stays unreviewed keeps matching, and
     * the reminder is sent exactly once because its idempotence id is built from the submission id and
     * does not change from night to night. That also covers a night the job did not run.
     */
    @Query(
        """
        SELECT new ch.admin.bj.swiyu.core.business.common.email.PendingReviewSubmission(
            e.id, e.partnerId)
        FROM TrustOnboardingSubmission e
        WHERE e.status = 'SUBMITTED'
          AND e.submittedAt IS NOT NULL
          AND e.submittedAt <= :cutoff
        ORDER BY e.id
        """
    )
    Page<PendingReviewSubmission> findPendingReviewSubmittedBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    void deleteByPartnerId(UUID partnerId);

    List<TrustOnboardingSubmission> findAllByPartnerIdOrderByInitiatedAtAsc(@NotNull UUID partnerId);
}
