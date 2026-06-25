package ch.admin.bj.swiyu.core.business.modules.status.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.didresolver")
public record DidResolverProperties(@Valid @NotNull DataSize maxDidLogSize) {}
