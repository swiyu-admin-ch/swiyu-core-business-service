package ch.admin.bj.swiyu.core.business.common.antivirus;

import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystem;
import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystemException;
import io.micrometer.core.annotation.Timed;
import java.net.URISyntaxException;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntivirusClient {

    private final ScanApi antivirusScanApi;

    @Timed
    public AntivirusScanResult scanURl(URL url) throws ExternalSystemException {
        ScanResult scanResult;
        try {
            var result = antivirusScanApi.scanGet(url.toURI());
            scanResult = result.getFirst();
        } catch (URISyntaxException | RestClientResponseException | ResourceAccessException e) {
            throw new ExternalSystemException(
                "Error in antivirus connectivity.",
                ExternalSystem.ANTIVIRUS_SERVICE,
                HttpStatus.SERVICE_UNAVAILABLE,
                e
            );
        }
        if (scanResult.getResult() == null || scanResult.getRequestID() == null) {
            throw new ExternalSystemException(
                "External system did respond with unexpected data. result: %s RequestID:%s".formatted(
                    scanResult.getResult(),
                    scanResult.getRequestID()
                ),
                ExternalSystem.ANTIVIRUS_SERVICE,
                HttpStatus.SERVICE_UNAVAILABLE
            );
        }
        return new AntivirusScanResult(
            scanResult.getRequestID().toString(),
            scanResult.getResult().compareTo("FOUND") == 0
        );
    }
}
