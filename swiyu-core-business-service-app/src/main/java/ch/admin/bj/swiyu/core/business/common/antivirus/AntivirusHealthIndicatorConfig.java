package ch.admin.bj.swiyu.core.business.common.antivirus;

import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Configuration
@RequiredArgsConstructor
class AntivirusHealthIndicatorConfig {

    private final ScanApi scanApi;

    /**
     * The Antivirus scanner does not provide a builtin check for health.
     * To still get an indication about the system we added a check to at least see if an
     * HTTP server does respond to our requests.
     * <p/>
     * We had to add a /health endpoint to the spec as all existing endpoints respond with a
     * 500 error code on a client issue (like not providing a File/URL to check).
     */
    @Bean
    public HealthIndicator antivirusHealthIndicator() {
        return () -> {
            try {
                log.debug("checking health of antivirus service...");
                scanApi.healthGet();
            } catch (RestClientResponseException e) {
                // 404 is the expected http code
                if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                    log.error("health check failed for antivirus service, but HTTP server response found.", e);
                    return Health.down().withException(e).build();
                }
            } catch (Exception e) {
                log.error("health check failed for antivirus service", e);
                return Health.down().withException(e).build();
            }
            log.debug("antivirus service is healthy");
            return Health.up().build();
        };
    }
}
