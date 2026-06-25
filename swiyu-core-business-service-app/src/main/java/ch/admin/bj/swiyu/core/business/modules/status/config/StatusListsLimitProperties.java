package ch.admin.bj.swiyu.core.business.modules.status.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.limits.statuslists")
public record StatusListsLimitProperties(
    @Valid @NotNull long defaultMaxCount,
    @Valid @NotNull DataSize minSize,
    @Valid @NotNull DataSize maxSize,
    @Valid @NotNull Duration maxAge
) {}
