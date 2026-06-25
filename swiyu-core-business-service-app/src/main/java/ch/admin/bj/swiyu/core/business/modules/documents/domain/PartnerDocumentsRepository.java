package ch.admin.bj.swiyu.core.business.modules.documents.domain;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerDocumentsRepository extends JpaRepository<PartnerDocument, UUID> {
    Page<PartnerDocument> findAllByTrustOnboardingSubmissionId(UUID trustOnboardingSubmissionId, Pageable pageable);

    List<PartnerDocument> findAllByTrustOnboardingSubmissionId(UUID trustOnboardingSubmissionId);

    int countPartnerDocumentsByTrustOnboardingSubmissionId(UUID trustOnboardingSubmissionId);

    int countPartnerDocumentsByPartnerId(@NotNull UUID partnerId);

    @Query(
        value = """
        SELECT pd.id FROM partner_document pd
        JOIN trust_onboarding_submission ts ON pd.trust_onboarding_submission_id = ts.id
            WHERE ts.status = 'UNSUBMITTED_TIMEOUT'
        """,
        nativeQuery = true
    )
    List<UUID> findAllIdsBelongingToTrustOnboardingSubmissionsInCleanupStatus();

    List<PartnerDocument> findAllByPartnerId(UUID partnerId);
}
