package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystem;
import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystemException;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties.SaferpayProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayAmount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayAssertRequest;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayAssertResponse;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayCancelRequest;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayCancelResponse;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayCaptureRequest;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayCaptureResponse;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayInitializeRequest;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayInitializeResponse;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayPayment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayRequestHeader;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayReturnUrl;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "saferpay.mock", havingValue = "false", matchIfMissing = true)
@Slf4j
public class DefaultSaferpayClient implements SaferpayClient {

    /** Version of the Saferpay JSON API the request/response classes in {@code model} are written against. */
    private static final String SPEC_VERSION = "1.44";

    /**
     * Path of the operation, relative to the configured base url. Deliberately not configurable: it is
     * part of the API contract (same on Saferpay TEST and PROD) and belongs to the request/response
     * classes. Only the base url differs per stage, see {@code payment.saferpay.base-url}.
     */
    @SuppressWarnings("java:S1075") // part of the Saferpay API contract, see above
    private static final String INITIALIZE_PATH = "/Payment/v1/PaymentPage/Initialize";

    @SuppressWarnings("java:S1075")
    private static final String ASSERT_PATH = "/Payment/v1/PaymentPage/Assert";

    @SuppressWarnings("java:S1075")
    private static final String CAPTURE_PATH = "/Payment/v1/Transaction/Capture";

    @SuppressWarnings("java:S1075")
    private static final String CANCEL_PATH = "/Payment/v1/Transaction/Cancel";

    private final RestClient saferpayRestClient;
    private final SaferpayProperties saferpayProperties;

    public DefaultSaferpayClient(
        @Qualifier("saferpayRestClient") RestClient saferpayRestClient,
        PaymentProperties paymentProperties
    ) {
        this.saferpayRestClient = saferpayRestClient;
        this.saferpayProperties = paymentProperties.saferpay();
    }

    @Override
    public SaferpayInitializeResult initialize(SaferpayInitializePaymentCommand command) {
        try {
            var response = saferpayRestClient
                .post()
                .uri(INITIALIZE_PATH)
                .body(buildRequest(command))
                .retrieve()
                .body(SaferpayInitializeResponse.class);
            if (response == null) {
                throw new ExternalSystemException(
                    "Error with system 'SAFERPAY' during PaymentPage/Initialize: received an empty response body.",
                    ExternalSystem.SAFERPAY,
                    HttpStatus.BAD_GATEWAY
                );
            }
            return new SaferpayInitializeResult(
                response.token(),
                response.redirectUrl(),
                Instant.parse(response.expiration())
            );
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Error with system 'SAFERPAY' during PaymentPage/Initialize.",
                ExternalSystem.SAFERPAY,
                e.getStatusCode(),
                e
            );
        } catch (ResourceAccessException e) {
            // no response at all: connection refused, timeout, TLS handshake failed, ...
            throw new ExternalSystemException(
                "Error with system 'SAFERPAY' during PaymentPage/Initialize: Saferpay could not be reached.",
                ExternalSystem.SAFERPAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                e
            );
        }
    }

    @Override
    public SaferpayAssertResult assertPayment(SaferpayAssertCommand command) {
        try {
            var request = buildAssertRequest(command);
            var response = saferpayRestClient
                .post()
                .uri(ASSERT_PATH)
                .body(request)
                .retrieve()
                .body(SaferpayAssertResponse.class);
            if (response == null) {
                throw new ExternalSystemException(
                    "Error with system 'SAFERPAY' during PaymentPage/Assert: received an empty response body.",
                    ExternalSystem.SAFERPAY,
                    HttpStatus.BAD_GATEWAY
                );
            }
            return new SaferpayAssertResult(response.transaction(), response.liability());
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Error with system 'SAFERPAY' during PaymentPage/Assert.",
                ExternalSystem.SAFERPAY,
                e.getStatusCode(),
                e
            );
        } catch (ResourceAccessException e) {
            throw new ExternalSystemException(
                "Error with system 'SAFERPAY' during PaymentPage/Assert: Saferpay could not be reached.",
                ExternalSystem.SAFERPAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                e
            );
        }
    }

    @Override
    public SaferpayCaptureResult captureTransaction(SaferpayCaptureCommand command) {
        try {
            var request = buildCaptureRequest(command);
            var response = saferpayRestClient
                .post()
                .uri(CAPTURE_PATH)
                .body(request)
                .retrieve()
                .body(SaferpayCaptureResponse.class);
            if (response == null) {
                throw new ExternalSystemException(
                    "Error with system 'SAFERPAY' during Transaction/Capture: received an empty response body.",
                    ExternalSystem.SAFERPAY,
                    HttpStatus.BAD_GATEWAY
                );
            }
            return new SaferpayCaptureResult(response.transaction());
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Error with system 'SAFERPAY' during Transaction/Capture.",
                ExternalSystem.SAFERPAY,
                e.getStatusCode(),
                e
            );
        } catch (ResourceAccessException e) {
            throw new ExternalSystemException(
                "Error with system 'SAFERPAY' during Transaction/Capture: Saferpay could not be reached.",
                ExternalSystem.SAFERPAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                e
            );
        }
    }

    @Override
    public SaferpayCancelResult cancelTransaction(SaferpayCancelCommand command) {
        try {
            var request = buildCancelRequest(command);
            var response = saferpayRestClient
                .post()
                .uri(CANCEL_PATH)
                .body(request)
                .retrieve()
                .body(SaferpayCancelResponse.class);
            if (response == null) {
                throw new ExternalSystemException(
                    "Error with system 'SAFERPAY' during Transaction/Cancel: received an empty response body.",
                    ExternalSystem.SAFERPAY,
                    HttpStatus.BAD_GATEWAY
                );
            }
            return new SaferpayCancelResult(response.transaction());
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Error with system 'SAFERPAY' during Transaction/Cancel.",
                ExternalSystem.SAFERPAY,
                e.getStatusCode(),
                e
            );
        } catch (ResourceAccessException e) {
            throw new ExternalSystemException(
                "Error with system 'SAFERPAY' during Transaction/Cancel: Saferpay could not be reached.",
                ExternalSystem.SAFERPAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                e
            );
        }
    }

    private SaferpayInitializeRequest buildRequest(SaferpayInitializePaymentCommand command) {
        var header = new SaferpayRequestHeader(
            SPEC_VERSION,
            saferpayProperties.customerId(),
            UUID.randomUUID().toString(),
            0
        );
        var payment = new SaferpayPayment(
            new SaferpayAmount(command.amountValue(), command.currency()),
            command.orderId(),
            command.description()
        );
        return new SaferpayInitializeRequest(
            header,
            saferpayProperties.terminalId(),
            payment,
            saferpayProperties.configSet(),
            new SaferpayReturnUrl(command.returnUrl())
        );
    }

    private SaferpayAssertRequest buildAssertRequest(SaferpayAssertCommand command) {
        var header = new SaferpayRequestHeader(
            SPEC_VERSION,
            saferpayProperties.customerId(),
            UUID.randomUUID().toString(),
            0
        );
        return new SaferpayAssertRequest(header, command.token());
    }

    private SaferpayCaptureRequest buildCaptureRequest(SaferpayCaptureCommand command) {
        var header = new SaferpayRequestHeader(
            SPEC_VERSION,
            saferpayProperties.customerId(),
            UUID.randomUUID().toString(),
            0
        );
        return new SaferpayCaptureRequest(header, command.transactionReference());
    }

    private SaferpayCancelRequest buildCancelRequest(SaferpayCancelCommand command) {
        var header = new SaferpayRequestHeader(
            SPEC_VERSION,
            saferpayProperties.customerId(),
            UUID.randomUUID().toString(),
            0
        );
        return new SaferpayCancelRequest(header, command.transactionReference());
    }
}
