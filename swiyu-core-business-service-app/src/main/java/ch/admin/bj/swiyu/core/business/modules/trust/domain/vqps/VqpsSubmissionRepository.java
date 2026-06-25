package ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VqpsSubmissionRepository extends JpaRepository<VqpsSubmission, UUID> {
    Page<VqpsSubmission> findAllByPartnerId(UUID partnerId, Pageable pageable);
}
