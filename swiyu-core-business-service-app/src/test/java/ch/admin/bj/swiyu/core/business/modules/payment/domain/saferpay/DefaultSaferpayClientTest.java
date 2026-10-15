package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystemException;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties.SaferpayProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayInitializeResponse;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class DefaultSaferpayClientTest {

    private final RestClient restClient = mock(RestClient.class);
    private final RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
    private final RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
    private final RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

    private final DefaultSaferpayClient client = clientWithConfigSet("cbs-payment-test");

    private DefaultSaferpayClient clientWithConfigSet(String configSet) {
        return new DefaultSaferpayClient(
            restClient,
            new PaymentProperties(
                List.of(),
                new SaferpayProperties(
                    "terminal-1",
                    "customer-1",
                    URI.create("https://test.saferpay.com/api"),
                    configSet
                ),
                List.of()
            )
        );
    }

    void stubRestClientChain() {
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        // body(Object) is overloaded with body(StreamingHttpOutputMessage.Body); any(Object.class)
        // is needed to pin the matcher to the Object overload actually used in production code.
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void initialize_emptyResponseBody_throwsExternalSystemExceptionInsteadOfNpe() {
        stubRestClientChain();
        when(responseSpec.body(SaferpayInitializeResponse.class)).thenReturn(null);

        var command = new SaferpayInitializePaymentCommand("P-AAAAAAAA", 15_000, "CHF", "desc", "https://return");

        assertThatThrownBy(() -> client.initialize(command)).isInstanceOf(ExternalSystemException.class);
    }

    @Test
    void initialize_saferpayNotReachable_throwsExternalSystemExceptionServiceUnavailable() {
        stubRestClientChain();
        when(responseSpec.body(SaferpayInitializeResponse.class)).thenThrow(
            new ResourceAccessException("Connection refused")
        );

        var command = new SaferpayInitializePaymentCommand("P-AAAAAAAA", 15_000, "CHF", "desc", "https://return");

        assertThatThrownBy(() -> client.initialize(command))
            .isInstanceOf(ExternalSystemException.class)
            .extracting(e -> ((ExternalSystemException) e).getHttpStatusCode())
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void initialize_validResponse_returnsResult() {
        stubRestClientChain();
        when(responseSpec.body(SaferpayInitializeResponse.class)).thenReturn(
            new SaferpayInitializeResponse("token-1", "https://saferpay.example/redirect", "2026-01-01T00:00:00Z")
        );

        var result = client.initialize(
            new SaferpayInitializePaymentCommand("P-AAAAAAAA", 15_000, "CHF", "desc", "https://return")
        );

        assertThat(result.token()).isEqualTo("token-1");
        assertThat(result.redirectUrl()).isEqualTo("https://saferpay.example/redirect");
    }

    @Test
    void initialize_configSetConfigured_sendsConfigSet() {
        assertThat(initializeAndSerializeRequest(client)).contains("\"ConfigSet\":\"cbs-payment-test\"");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void initialize_noConfigSetConfigured_omitsConfigSet(String configSet) {
        assertThat(initializeAndSerializeRequest(clientWithConfigSet(configSet))).doesNotContain("ConfigSet");
    }

    private String initializeAndSerializeRequest(DefaultSaferpayClient saferpayClient) {
        stubRestClientChain();
        when(responseSpec.body(SaferpayInitializeResponse.class)).thenReturn(
            new SaferpayInitializeResponse("token-1", "https://saferpay.example/redirect", "2026-01-01T00:00:00Z")
        );

        saferpayClient.initialize(
            new SaferpayInitializePaymentCommand("P-AAAAAAAA", 15_000, "CHF", "desc", "https://return")
        );

        var bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(bodySpec).body(bodyCaptor.capture());
        return JsonMapper.builder().build().writeValueAsString(bodyCaptor.getValue());
    }
}
