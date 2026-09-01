package ch.admin.bj.swiyu.core.business.modules.management.domain;

import ch.admin.bj.swiyu.core.business.common.email.ExpiringPartnerIdentity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessPartnerRepository extends JpaRepository<BusinessEntity, UUID> {
    Page<BusinessEntity> findAllByIdIn(List<UUID> ids, Pageable pageable);

    /**
     * Partners whose active Trust Identity expires inside the given window, ordered so that paging is
     * stable while the job walks the pages.
     *
     * <p>A window rather than an exact day: the job runs once a night, and a night it does not run -
     * deployment, outage, a pod that was down - would otherwise drop that day's reminders for good.
     * Re-sending is prevented by the idempotence id, not by the query being narrow.
     */
    @Query(
        """
        SELECT new ch.admin.bj.swiyu.core.business.common.email.ExpiringPartnerIdentity(
            b.id, b.businessPartnerIdentity.validUntil)
        FROM BusinessEntity b
        WHERE b.businessPartnerIdentity.status = 'ACTIVE'
          AND b.businessPartnerIdentity.validUntil > :windowStart
          AND b.businessPartnerIdentity.validUntil <= :windowEnd
        ORDER BY b.id
        """
    )
    Page<ExpiringPartnerIdentity> findIdentitiesExpiringBetween(
        @Param("windowStart") Instant windowStart,
        @Param("windowEnd") Instant windowEnd,
        Pageable pageable
    );
}
