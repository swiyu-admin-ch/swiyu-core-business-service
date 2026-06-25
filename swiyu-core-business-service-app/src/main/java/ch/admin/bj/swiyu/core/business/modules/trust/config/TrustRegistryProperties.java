package ch.admin.bj.swiyu.core.business.modules.trust.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.trust-registry")
public record TrustRegistryProperties(@Valid @NotNull URL dataServiceBaseUrl) {}
