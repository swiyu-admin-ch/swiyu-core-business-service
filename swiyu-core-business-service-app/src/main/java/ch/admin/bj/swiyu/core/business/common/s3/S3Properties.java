package ch.admin.bj.swiyu.core.business.common.s3;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "s3")
public record S3Properties(Bucket trustOnboardingSubmissionDocuments) {
    public List<String> getAllBucketNames() {
        return List.of(trustOnboardingSubmissionDocuments().bucketName());
    }

    @Validated
    public record Bucket(
        @NotEmpty String bucketName,
        @NotEmpty String documentGroup,
        @Valid @NotNull Duration expiryOfPreSignedLink
    ) {}
}
