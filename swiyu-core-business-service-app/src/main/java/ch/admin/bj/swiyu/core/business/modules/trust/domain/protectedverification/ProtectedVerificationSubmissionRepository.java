package ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ProtectedVerificationSubmissionRepository
    extends
        JpaRepository<ProtectedVerificationSubmission, UUID>,
        QuerydslPredicateExecutor<ProtectedVerificationSubmission>
{
    void deleteByPartnerId(UUID partnerId);
}
