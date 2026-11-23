package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.TiPaymentChangeNotificationReceivedEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/** The job with the real outbox and ShedLock. Batch size is 2 in the test profile. */
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
@WithExtendedJeapAuthenticationToken
class PaymentReconciliationJobIT {

    private static final String LOCK_NAME = "PaymentReconciliationJob";

    @Autowired
    PaymentReconciliationJob job;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @Autowired
    TestRepositories repos;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    LockProvider lockProvider;

    @BeforeEach
    void setUp() {
        repos.payment.deleteAllInBatch();
        // the outbox repository needs a transaction
        transactionTemplate.executeWithoutResult(status -> {
            var messages = deferredMessageRepository.findAll();
            deferredMessageRepository.deleteAllById(
                messages.stream().map(DeferredMessage::getId).collect(Collectors.toSet())
            );
        });
    }

    @Test
    void pendingPaymentWithExpiredSession_publishesChangeNotification() {
        pendingPayment(Duration.ofMinutes(40), Duration.ofMinutes(-10));

        job.reconcilePendingPayments();

        assertThat(publishedTopics()).containsExactly(TiPaymentChangeNotificationReceivedEvent.TypeRef.DEFAULT_TOPIC);
    }

    @Test
    void pendingPaymentWithRunningSession_isIgnored() {
        // the user may still be paying
        pendingPayment(Duration.ofMinutes(30), Duration.ofMinutes(10));

        job.reconcilePendingPayments();

        assertThat(publishedTopics()).isEmpty();
    }

    @Test
    void morePaymentsToReconcileThanTheBatchSize_handlesAllInOneRun() {
        pendingPayment(Duration.ofMinutes(40), Duration.ofMinutes(-10));
        pendingPayment(Duration.ofMinutes(50), Duration.ofMinutes(-20));
        pendingPayment(Duration.ofMinutes(60), Duration.ofMinutes(-30));

        job.reconcilePendingPayments();

        // batch size is 2 in the tests
        assertThat(publishedTopics()).hasSize(3);
    }

    @Test
    void lockHeldByAnotherPod_doesNothing() {
        pendingPayment(Duration.ofMinutes(40), Duration.ofMinutes(-10));
        // simulates another pod running the job right now
        var otherPodsLock = lockProvider
            .lock(new LockConfiguration(Instant.now(), LOCK_NAME, Duration.ofMinutes(5), Duration.ZERO))
            .orElseThrow();

        job.reconcilePendingPayments();

        assertThat(publishedTopics()).isEmpty();

        // once the other pod is done, the next run picks the payment up
        otherPodsLock.unlock();
        job.reconcilePendingPayments();

        assertThat(publishedTopics()).hasSize(1);
    }

    /**
     * PENDING payment created {@code age} ago, session expires in {@code expiresIn} (negative = expired).
     * Own business partner each, as only one PENDING payment per partner and product is allowed.
     */
    private void pendingPayment(Duration age, Duration expiresIn) {
        var payment = new Payment(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/payment"
        );
        payment.applySaferpayInitialization("token", Instant.now().plus(expiresIn), "https://saferpay/redirect");
        repos.payment.saveAndFlush(payment);
        // backdate createdAt (set by auditing); Hibernate stores Instants as UTC
        jdbcTemplate.update(
            "UPDATE payment SET created_at = ? WHERE id = ?",
            LocalDateTime.ofInstant(Instant.now().minus(age), ZoneOffset.UTC),
            payment.getId()
        );
    }

    private List<String> publishedTopics() {
        return deferredMessageRepository.findAll().stream().map(DeferredMessage::getTopic).toList();
    }
}
