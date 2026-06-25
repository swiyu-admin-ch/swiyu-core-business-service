package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PamsHealthIndicatorConfig {

    private final PamsClient pamsClient;

    @Bean
    public HealthIndicator pamsHealthIndicator() {
        return () -> {
            try {
                log.debug("checking health of pams api service...");
                pamsClient.getHealth();

                log.debug("pams api service is healthy");
                return Health.up().build();
            } catch (Exception e) {
                log.error("health check failed for pams api service", e);
                return Health.down().withException(e).build();
            }
        };
    }
}
