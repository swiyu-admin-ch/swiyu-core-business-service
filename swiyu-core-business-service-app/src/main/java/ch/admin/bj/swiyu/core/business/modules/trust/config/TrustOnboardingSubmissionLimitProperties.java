package ch.admin.bj.swiyu.core.business.modules.trust.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.limits.trust-onboarding-submission")
public record TrustOnboardingSubmissionLimitProperties(
    @Valid @NotNull TrustOnboardingSubmissionDocumentLimitProperties documents,
    @Valid @NotNull Duration maxAgeInUnsubmitted
) {
    @Validated
    public record TrustOnboardingSubmissionDocumentLimitProperties(
        @Valid @NotNull long defaultMaxCountPerSubmission,
        @Valid @NotNull long defaultMaxCountPerBusinessPartner,
        @Valid @NotNull DataSize minFileSize,
        @Valid @NotNull DataSize maxFileSize,
        @NotEmpty List<String> allowedContentTypes
    ) {}
}
