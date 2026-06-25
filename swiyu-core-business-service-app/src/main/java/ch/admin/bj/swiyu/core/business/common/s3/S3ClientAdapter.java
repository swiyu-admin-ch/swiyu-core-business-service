package ch.admin.bj.swiyu.core.business.common.s3;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ClientAdapter {

    private final S3Presigner internalS3Presigner;
    private final S3Client client;

    public void createBucket(String bucketName) {
        try {
            log.debug("creating bucket {} ...", bucketName);
            var s3Waiter = client.waiter();
            var bucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();

            client.createBucket(bucketRequest);
            var bucketRequestWait = HeadBucketRequest.builder().bucket(bucketName).build();

            // Wait until the bucket is created
            s3Waiter.waitUntilBucketExists(bucketRequestWait);
        } catch (S3Exception e) {
            throw new IllegalStateException("could not create bucket", e);
        }
    }

    public boolean bucketExists(String bucketName) {
        var headBucketRequest = HeadBucketRequest.builder().bucket(bucketName).build();
        try {
            client.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException _) {
            return false;
        }
    }

    public boolean fileExists(String bucket, String key) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public void enableVersioning(String bucket) {
        try {
            log.debug("enabling versioning for bucket {} ...", bucket);

            var request = PutBucketVersioningRequest.builder()
                .bucket(bucket)
                .versioningConfiguration(versioningConfiguration ->
                    versioningConfiguration.status(BucketVersioningStatus.ENABLED)
                )
                .build();

            client.putBucketVersioning(request);
        } catch (S3Exception e) {
            throw new IllegalStateException("failed to set versioning on bucket", e);
        }
    }

    public boolean isVersioningEnabled(String bucket) {
        try {
            log.debug("check versioning on bucket {} ...", bucket);
            var request = GetBucketVersioningRequest.builder().bucket(bucket).build();

            var response = client.getBucketVersioning(request);
            return BucketVersioningStatus.ENABLED.equals(response.status());
        } catch (S3Exception e) {
            throw new IllegalStateException("failed to get versioning on bucket", e);
        }
    }

    /**
     * @param expirationDuration Dauer wie lange das Dokument unter der zurückgegebenen Url zugreifbar ist.
     */
    public URL generatePresignedDownloadUrl(String bucket, String objectKey, Duration expirationDuration) {
        var presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expirationDuration)
            .getObjectRequest(objectRequest -> objectRequest.bucket(bucket).key(objectKey))
            .build();

        log.debug("generating presigned download url for internal bucket {} ...", bucket);
        return internalS3Presigner.presignGetObject(presignRequest).url();
    }

    public ResponseInputStream<GetObjectResponse> getObject(String bucket, String objectKey) {
        var objectRequest = GetObjectRequest.builder().bucket(bucket).key(objectKey).build();
        return this.client.getObject(objectRequest);
    }

    public PutObjectResponse upload(String bucket, String objectKey, byte[] file) {
        var putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(objectKey).build();

        return client.putObject(putObjectRequest, RequestBody.fromBytes(file));
    }

    public DeleteObjectResponse deleteObject(String bucket, String objectKey) {
        var deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build();

        return client.deleteObject(deleteObjectRequest);
    }

    /**
     * Returns all S3Objects, even when there are more than 1000 objects. So be careful when using
     * this method.
     */
    public List<S3Object> listObjects(String bucketName) {
        List<S3Object> result = new ArrayList<>();

        try {
            var listObjectsRequest = ListObjectsV2Request.builder().bucket(bucketName).build();

            ListObjectsV2Response listObjectsResponse = null;
            String nextContinuationToken = null;

            do {
                if (listObjectsResponse != null) {
                    listObjectsRequest = listObjectsRequest
                        .toBuilder()
                        .continuationToken(nextContinuationToken)
                        .build();
                }

                listObjectsResponse = client.listObjectsV2(listObjectsRequest);
                nextContinuationToken = listObjectsResponse.nextContinuationToken();

                var chunk = listObjectsResponse.contents();
                result.addAll(chunk);
            } while (Boolean.TRUE.equals(listObjectsResponse.isTruncated()));
        } catch (S3Exception e) {
            log.error(e.awsErrorDetails().errorMessage());
        }

        return result;
    }
}
