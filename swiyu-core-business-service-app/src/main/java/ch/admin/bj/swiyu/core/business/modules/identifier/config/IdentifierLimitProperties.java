package ch.admin.bj.swiyu.core.business.modules.identifier.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.limits.identifier")
public record IdentifierLimitProperties(
    @Valid @NotNull long defaultMaxCount,
    @Valid DidDocLimits didDoc,
    @Valid DidLogLimits didLog
) {
    public record DidDocLimits(@Valid DataSize maxSize) {}

    public record DidLogLimits(@Valid DataSize maxSize) {}
}
