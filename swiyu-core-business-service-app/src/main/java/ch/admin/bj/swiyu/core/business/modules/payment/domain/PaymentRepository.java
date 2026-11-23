package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, QuerydslPredicateExecutor<Payment> {
    boolean existsByBusinessPartnerIdAndProductAndStatus(
        UUID businessPartnerId,
        ProductType product,
        PaymentStatus status
    );

    Optional<Payment> findByReferenceId(String referenceId);

    Optional<Payment> findById(UUID id);

    /**
     * Loads a payment only if it belongs to the given business partner, so a caller can never act on
     * another business partner's payment by guessing its id.
     */
    Optional<Payment> findByIdAndBusinessPartnerId(UUID id, UUID businessPartnerId);

    /**
     * Ids of the PENDING payments for the reconciliation job, oldest first: their Saferpay session expired
     * before {@code cutoff} (so the user is done paying), or they have no session and were created before
     * {@code cutoff}. Only payments created after {@code createdAfter}.
     */
    @Query(
        """
        select p.id from Payment p
        where p.status = ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus.PENDING
          and p.auditMetadata.createdAt > :createdAfter
          and (p.saferpayExpiration < :cutoff
               or (p.saferpayExpiration is null and p.auditMetadata.createdAt < :cutoff))
        order by p.auditMetadata.createdAt, p.id
        """
    )
    Slice<UUID> findPendingPaymentIdsToReconcile(
        @Param("cutoff") Instant cutoff,
        @Param("createdAfter") Instant createdAfter,
        Pageable pageable
    );
}
