package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

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
}
