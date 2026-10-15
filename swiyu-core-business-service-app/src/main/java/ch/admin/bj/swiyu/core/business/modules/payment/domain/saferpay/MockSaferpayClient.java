package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

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
}
