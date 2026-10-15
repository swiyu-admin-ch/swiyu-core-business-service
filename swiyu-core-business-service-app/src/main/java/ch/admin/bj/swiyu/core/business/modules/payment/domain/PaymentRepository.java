package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsByBusinessPartnerIdAndProductAndStatus(
        UUID businessPartnerId,
        ProductType product,
        PaymentStatus status
    );
}
