package ch.admin.bj.swiyu.core.business.common.antivirus;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jeap.antivirus-service")
record AntivirusProperties(@NotEmpty String url, @NotEmpty String username, @NotEmpty String password) {
    public String getBaseUrl() {
        if (url.endsWith("/scan")) {
            return url.substring(0, url.length() - 5);
        }
        return url;
    }
}
