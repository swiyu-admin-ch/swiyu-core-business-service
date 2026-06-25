package ch.admin.bj.swiyu.core.business.common.s3;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class S3HealthIndicator implements HealthIndicator {

    private final S3ClientAdapter s3;
    private final S3Properties s3Config;

    @Override
    public Health health() {
        log.debug("invoke S3 health check");
        try {
            executeHealthCheck();
            return Health.up().build();
        } catch (Exception e) {
            log.warn("S3 health check failed with: {} ", e.getMessage());
            return Health.down(e).build();
        }
    }

    private void executeHealthCheck() {
        s3Config
            .getAllBucketNames()
            .forEach(bucket -> {
                if (!s3.bucketExists(bucket)) {
                    throw new IllegalStateException("Bucket %s could not be accessed".formatted(bucket));
                }
            });
    }
}
