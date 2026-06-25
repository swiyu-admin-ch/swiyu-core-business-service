package ch.admin.bj.swiyu.core.business.modules.management.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessPartnerRepository extends JpaRepository<BusinessEntity, UUID> {
    Page<BusinessEntity> findAllByIdIn(List<UUID> ids, Pageable pageable);
}
