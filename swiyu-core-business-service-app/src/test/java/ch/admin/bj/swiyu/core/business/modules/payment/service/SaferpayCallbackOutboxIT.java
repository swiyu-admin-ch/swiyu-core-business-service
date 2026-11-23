package ch.admin.bj.swiyu.core.business.modules.payment.service;

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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/** The Saferpay callback with the real outbox, which needs a transaction, a contract and an Avro key. */
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
@WithExtendedJeapAuthenticationToken
class SaferpayCallbackOutboxIT {

    @Autowired
    PaymentService paymentService;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @Autowired
    TestRepositories repos;

    @Autowired
    TransactionTemplate transactionTemplate;

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
    void callbackForExistingPayment_publishesChangeNotification() {
        repos.payment.saveAndFlush(
            new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "P-CB000001",
                ProductType.BASE_REGISTRATION,
                Amount.chf(15_000),
                "https://portal.example.local/payment"
            )
        );

        // no surrounding transaction, like the controller
        paymentService.handleSaferpayCallback("P-CB000001");

        assertThat(publishedTopics()).containsExactly(TiPaymentChangeNotificationReceivedEvent.TypeRef.DEFAULT_TOPIC);
    }

    @Test
    void callbackForUnknownPayment_publishesNothing() {
        paymentService.handleSaferpayCallback("P-UNKNOWN1");

        assertThat(publishedTopics()).isEmpty();
    }

    private List<String> publishedTopics() {
        return deferredMessageRepository.findAll().stream().map(DeferredMessage::getTopic).toList();
    }
}
