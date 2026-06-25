package ch.admin.bj.swiyu.core.business.modules.trust.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.limits.trust-onboarding-submission.proof-of-possession")
public record TrustOnboardingSubmissionProofOfPossessionProperties(@NotNull Duration issuanceToValidityDuration) {}
