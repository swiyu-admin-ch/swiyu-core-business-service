package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_B;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.insertTestBusinessPartners;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken
class PaymentRepositoryTest {

    @Autowired
    TestRepositories repos;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        insertTestBusinessPartners(repos.businessPartner);
        repos.payment.deleteAll();
    }

    @Test
    void findByReferenceId_existing_returnsPayment() {
        var payment = createTestPayment("P-ABC12345");
        repos.payment.saveAndFlush(payment);

        var found = repos.payment.findByReferenceId("P-ABC12345");

        assertThat(found).isPresent();
        assertThat(found.get().getReferenceId()).isEqualTo("P-ABC12345");
    }

    @Test
    void findByReferenceId_notExisting_returnsEmpty() {
        var found = repos.payment.findByReferenceId("P-NOTEXIST");

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdAndBusinessPartnerId_matchingPartner_returnsPayment() {
        var payment = createTestPayment("P-ABC12345");
        repos.payment.saveAndFlush(payment);

        var found = repos.payment.findByIdAndBusinessPartnerId(payment.getId(), ENTITY_A);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(payment.getId());
    }

    @Test
    void findByIdAndBusinessPartnerId_otherPartner_returnsEmpty() {
        var payment = createTestPayment("P-ABC12345");
        repos.payment.saveAndFlush(payment);

        var found = repos.payment.findByIdAndBusinessPartnerId(payment.getId(), ENTITY_B);

        assertThat(found).isEmpty();
    }

    @Test
    void findPendingPaymentIdsToReconcile_onlyPendingPaymentsWithExpiredSessionInsideTheWindow_oldestFirst() {
        var now = Instant.now();
        // session running, the user may still be paying
        payment("P-00000001", Duration.ofMinutes(30), Duration.ofMinutes(10), PaymentStatus.PENDING);
        // expired, but inside the grace period
        payment("P-00000002", Duration.ofMinutes(30), Duration.ofMinutes(-2), PaymentStatus.PENDING);
        var expired = payment("P-00000003", Duration.ofMinutes(40), Duration.ofMinutes(-10), PaymentStatus.PENDING);
        var expiredLongAgo = payment("P-00000004", Duration.ofHours(23), Duration.ofHours(-22), PaymentStatus.PENDING);
        // outside the lookback window
        payment("P-00000005", Duration.ofHours(25), Duration.ofHours(-24), PaymentStatus.PENDING);
        // session expired, but already final
        payment("P-00000006", Duration.ofMinutes(40), Duration.ofMinutes(-10), PaymentStatus.FAILED);

        var found = repos.payment.findPendingPaymentIdsToReconcile(
            now.minus(Duration.ofMinutes(5)),
            now.minus(Duration.ofHours(24)),
            Pageable.ofSize(50)
        );

        assertThat(found).containsExactly(expiredLongAgo.getId(), expired.getId());
    }

    @Test
    void findPendingPaymentIdsToReconcile_withoutSession_qualifiesByCreationTime() {
        var now = Instant.now();
        // no session, e.g. Initialize result never stored
        var withoutSession = payment("P-00000001", Duration.ofMinutes(10), null, PaymentStatus.PENDING);
        // no session yet, just created
        payment("P-00000002", Duration.ofMinutes(2), null, PaymentStatus.PENDING);

        var found = repos.payment.findPendingPaymentIdsToReconcile(
            now.minus(Duration.ofMinutes(5)),
            now.minus(Duration.ofHours(24)),
            Pageable.ofSize(50)
        );

        assertThat(found).containsExactly(withoutSession.getId());
    }

    @Test
    void findPendingPaymentIdsToReconcile_respectsBatchSize() {
        var now = Instant.now();
        var oldest = payment("P-00000001", Duration.ofHours(3), Duration.ofHours(-2), PaymentStatus.PENDING);
        var middle = payment("P-00000002", Duration.ofHours(2), Duration.ofHours(-1), PaymentStatus.PENDING);
        payment("P-00000003", Duration.ofHours(1), Duration.ofMinutes(-30), PaymentStatus.PENDING);

        var found = repos.payment.findPendingPaymentIdsToReconcile(
            now.minus(Duration.ofMinutes(5)),
            now.minus(Duration.ofHours(24)),
            Pageable.ofSize(2)
        );

        assertThat(found).containsExactly(oldest.getId(), middle.getId());
    }

    /**
     * Payment created {@code age} ago, session expires in {@code expiresIn} (negative = expired, null = no
     * session). Own business partner each, as only one PENDING payment per partner and product is allowed.
     */
    private Payment payment(String referenceId, Duration age, Duration expiresIn, PaymentStatus status) {
        var payment = new Payment(
            UUID.randomUUID(),
            UUID.randomUUID(),
            referenceId,
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
        if (expiresIn != null) {
            payment.applySaferpayInitialization("token", Instant.now().plus(expiresIn), "https://saferpay/redirect");
        }
        if (status == PaymentStatus.FAILED) {
            payment.markAsFailed();
        }
        repos.payment.saveAndFlush(payment);
        // backdate createdAt (set by auditing); Hibernate stores Instants as UTC
        jdbcTemplate.update(
            "UPDATE payment SET created_at = ? WHERE id = ?",
            LocalDateTime.ofInstant(Instant.now().minus(age), ZoneOffset.UTC),
            payment.getId()
        );
        return payment;
    }

    private Payment createTestPayment(String referenceId) {
        return new Payment(
            UUID.randomUUID(),
            ENTITY_A,
            referenceId,
            ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType.BASE_REGISTRATION,
            ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
    }
}
