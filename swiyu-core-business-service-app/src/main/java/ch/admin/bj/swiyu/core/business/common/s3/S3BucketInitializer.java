package ch.admin.bj.swiyu.core.business.common.s3;

import ch.admin.bj.swiyu.core.business.common.async.AsyncService;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3BucketInitializer {

    private final S3Properties s3config;
    private final S3ClientAdapter s3;
    private final AsyncService async;

    @NewSpan // ansonsten fehlt traceid und spanid in logs
    @EventListener(classes = ApplicationReadyEvent.class)
    public void initS3StorageBuckets() {
        async.run(() -> {
            try {
                log.debug("Initializing S3 buckets...");
                for (var bucket : s3config.getAllBucketNames()) initBucket(bucket, false);
            } catch (S3Exception | IllegalStateException | SdkClientException e) {
                log.error("Failed to initialize S3 buckets. Was the S3ClientConfig correctly setup?", e);
            }
        });
    }

    private void initBucket(String bucketName, boolean versioning) {
        log.debug("Initializing bucket {} ...", bucketName);
        if (!s3.bucketExists(bucketName)) {
            log.debug("bucket {} does not exist. creating it...", bucketName);
            s3.createBucket(bucketName);
        }
        // Versioning: so deletes are only soft deletes and overrides keep previous version
        if (versioning && !s3.isVersioningEnabled(bucketName)) {
            s3.enableVersioning(bucketName);
        }
        log.debug("Done initializing bucket {}", bucketName);
    }
}
