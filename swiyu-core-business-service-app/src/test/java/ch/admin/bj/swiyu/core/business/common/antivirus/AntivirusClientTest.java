package ch.admin.bj.swiyu.core.business.common.antivirus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystemException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

class AntivirusClientTest {

    ScanApi antivirusScanApi = mock(ScanApi.class);

    AntivirusClient antivirusClient = new AntivirusClient(antivirusScanApi);

    @Test
    void testAntivirusScanSuccess() throws IOException, URISyntaxException {
        var id = UUID.randomUUID();
        var scanResult = new ScanResult()
            .result("OR")
            .filename("filename")
            .clamavDatabaseVersion("db-1")
            .clamavVersion("v1")
            .requestID(id)
            .description("description");
        when(antivirusScanApi.scanGet(any())).thenReturn(List.of(scanResult));

        var antivirusResult = antivirusClient.scanURl(new URI("http://test.url").toURL());
        assertThat(antivirusResult).isNotNull();
        assertThat(antivirusResult.virusFound()).isFalse();
        assertThat(antivirusResult.id()).isEqualTo(id.toString());
    }

    @Test
    void testAntivirusScanFailedByPayload() throws IOException, URISyntaxException {
        var id = UUID.randomUUID();
        var scanResult = new ScanResult()
            .result("FOUND")
            .filename("filename")
            .clamavDatabaseVersion("db-1")
            .clamavVersion("v1")
            .requestID(id)
            .description("description");
        when(antivirusScanApi.scanGet(any())).thenReturn(List.of(scanResult));

        var antivirusResult = antivirusClient.scanURl(new URI("http://test.url").toURL());
        assertThat(antivirusResult).isNotNull();
        assertThat(antivirusResult.virusFound()).isTrue();
        assertThat(antivirusResult.id()).isEqualTo(id.toString());
    }

    @Test
    void testScanFailedBy401() throws IOException, URISyntaxException {
        when(antivirusScanApi.scanGet(any())).thenThrow(
            new RestClientResponseException(
                "401 Authorization",
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                null, // response headers
                null, // response body as byte[]
                null // response charset
            )
        );
        var testUrl = new URI("http://test.url").toURL();
        assertThatThrownBy(() -> antivirusClient.scanURl(testUrl)).isInstanceOf(ExternalSystemException.class);
    }
}
