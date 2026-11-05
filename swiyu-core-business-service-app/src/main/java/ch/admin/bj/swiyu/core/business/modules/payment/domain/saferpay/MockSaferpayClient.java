package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayAmount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayBrand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayLiability;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayPaymentMeans;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayTransaction;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayTransactionReference;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stand-in for Saferpay, active with {@code saferpay.mock=true}. Even the Saferpay TEST environment is
 * only reachable with the mTLS client certificate and from a whitelisted source IP, which a developer
 * machine and the build pipeline do not have. The mock allows to start the application and to use the
 * payment endpoint there anyway. Stages with certificate and network access run with the mock disabled.
 */
@Component
@ConditionalOnProperty(name = "saferpay.mock", havingValue = "true")
@Slf4j
public class MockSaferpayClient implements SaferpayClient {

    public MockSaferpayClient() {
        log.info("Running with mocked saferpay client");
    }

    @Override
    public SaferpayInitializeResult initialize(SaferpayInitializePaymentCommand command) {
        log.debug("Mocking invocation of Saferpay PaymentPage/Initialize for orderId {}", command.orderId());
        return new SaferpayInitializeResult(
            "mock-token-" + command.orderId(),
            "https://mock.saferpay.local/redirect/" + command.orderId(),
            Instant.now().plus(Duration.ofMinutes(30))
        );
    }

    @Override
    public SaferpayAssertResult assertPayment(SaferpayAssertCommand command) {
        log.debug("Mocking invocation of Saferpay PaymentPage/Assert for token {}", command.token());
        var transaction = new SaferpayTransaction(
            "mock-txn-" + command.token(),
            "AUTHORIZED",
            "Payment",
            new SaferpayAmount(50_000, "CHF"),
            "CHF",
            Instant.now().toString(),
            command.token(),
            "Mock Acquirer",
            new SaferpayPaymentMeans(new SaferpayBrand("VISA")),
            new SaferpayTransactionReference("mock-txn-ref-" + command.token())
        );
        var liability = new SaferpayLiability(true);
        return new SaferpayAssertResult(transaction, liability);
    }

    @Override
    public SaferpayCaptureResult captureTransaction(SaferpayCaptureCommand command) {
        log.debug(
            "Mocking invocation of Saferpay Transaction/Capture for transactionReference {}",
            command.transactionReference().transactionId()
        );
        var transaction = new SaferpayTransaction(
            command.transactionReference().transactionId(),
            "CAPTURED",
            "Payment",
            new SaferpayAmount(50_000, "CHF"),
            "CHF",
            Instant.now().toString(),
            "mock-order",
            "Mock Acquirer",
            new SaferpayPaymentMeans(new SaferpayBrand("VISA")),
            new SaferpayTransactionReference(command.transactionReference().transactionId())
        );
        return new SaferpayCaptureResult(transaction);
    }

    @Override
    public SaferpayCancelResult cancelTransaction(SaferpayCancelCommand command) {
        log.debug(
            "Mocking invocation of Saferpay Transaction/Cancel for transactionReference {}",
            command.transactionReference().transactionId()
        );
        var transaction = new SaferpayTransaction(
            command.transactionReference().transactionId(),
            "CANCELLED",
            "Payment",
            new SaferpayAmount(50_000, "CHF"),
            "CHF",
            Instant.now().toString(),
            "mock-order",
            "Mock Acquirer",
            new SaferpayPaymentMeans(new SaferpayBrand("VISA")),
            new SaferpayTransactionReference(command.transactionReference().transactionId())
        );
        return new SaferpayCancelResult(transaction);
    }
}
