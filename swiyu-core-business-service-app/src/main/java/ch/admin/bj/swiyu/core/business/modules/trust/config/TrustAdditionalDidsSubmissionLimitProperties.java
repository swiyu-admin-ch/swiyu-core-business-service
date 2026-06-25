package ch.admin.bj.swiyu.core.business.modules.trust.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.limits.trust-additional-dids-submission")
public record TrustAdditionalDidsSubmissionLimitProperties(@Valid @NotNull Duration maxAgeInUnsubmitted) {}
