package ch.admin.bj.swiyu.core.business.modules.trust.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.limits.vqps-submission")
public record VqpsSubmissionLimitProperties(@Valid @NotNull DcqlQueryLimits dcqlQuery) {
    public record DcqlQueryLimits(@Positive int maxLength) {}
}
