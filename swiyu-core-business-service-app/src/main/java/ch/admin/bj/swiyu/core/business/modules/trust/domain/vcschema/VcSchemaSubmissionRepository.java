package ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VcSchemaSubmissionRepository extends JpaRepository<VcSchemaSubmission, UUID> {
    Page<VcSchemaSubmission> findAllByPartnerId(UUID id, Pageable pageable);
}
