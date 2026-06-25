package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pams")
public record PamsProperties(@NotNull PamsApiProperties api, @NotNull @NotEmpty String autoAppliedProfileGroupId) {
    public String createToken() {
        try {
            return PamsApplicationTokenGenerator.createToken(
                api.appId(),
                15, // PAMS-API according to https://confluence.bit.admin.ch/display/EPORTAL/ePortal+Security#ePortalSecurity-JWTSignature
                api.issuedBy(),
                // Add a margin to the TTL to prevent
                // that requests use an expired token while the token gets updated.
                api.tokenTtl(),
                api.userAgent(),
                JWK.parseFromPEMEncodedObjects(api.privateKey())
            );
        } catch (JOSEException e) {
            throw new IllegalStateException("Error with system 'PAMS' during creation of local token", e);
        }
    }

    public String getApiUrl() {
        return api.url().toString();
    }

    @Validated
    public record PamsApiProperties(
        @NotNull int appId,
        @NotEmpty String issuedBy,
        @NotNull Duration tokenTtl,
        @NotEmpty String userAgent,
        @NotEmpty(
            message = """
            Private key for accessing PAMS is not configured under 'pams.api.private-key'. Either configure
            the key or start the application with a mocked PAMS client using the 'mock-pams' profile as
            described in the readme.
            """
        ) String privateKey,
        @NotNull URI url
    ) {}
}
