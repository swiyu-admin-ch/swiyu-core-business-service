package ch.admin.bj.swiyu.core.business.common.audit;

import static ch.admin.bj.swiyu.core.business.common.audit.AuditorProvider.getCurrentAuditor;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.security.SystemUserAuthentication;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuditorProviderTest {

    @Test
    void getCurrentAuditor_eportal() {
        // GIVEN
        var auth = new JeapAuthenticationToken(jwtEportal(), emptySet(), emptyMap(), emptyList());
        auth.setAuthenticated(true);
        // WHEN
        var auditor = getCurrentAuditor(auth);
        // THEN
        assertFalse(auditor.isAnonymous());
        assertFalse(auditor.isSystem());
        assertEquals("06CB2415-8F57-4804-735B-DA1120B62C89:Mustermann,Max", auditor.auditUserId());
        assertEquals(
            "https://auth.trust-infra.swiyu-int.admin.ch/realms/bj-swiyu-ecosystem",
            auditor.identityProvider()
        );
    }

    @Test
    void getCurrentAuditor_apiGw() {
        // GIVEN
        var auth = new JeapAuthenticationToken(jwtApiGw(), emptySet(), emptyMap(), emptyList());
        auth.setAuthenticated(true);
        // WHEN
        var auditor = getCurrentAuditor(auth);
        // THEN
        assertFalse(auditor.isAnonymous());
        assertFalse(auditor.isSystem());
        assertEquals(
            "test-app_fbc9d4eb-cd87-4486-9e5a-08157940fbe0:system_user_of_business_partner",
            auditor.auditUserId()
        );
    }

    @Test
    void getCurrentAuditor_anonymous() {
        // GIVEN
        var auth = new JeapAuthenticationToken(jwtAnonymous(), emptySet(), emptyMap(), emptyList());
        auth.setAuthenticated(true);
        // WHEN
        var auditor = getCurrentAuditor(auth);
        // THEN
        assertTrue(auditor.isAnonymous());
        assertEquals("ANONYMOUS", auditor.auditUserId());
    }

    @Test
    void getCurrentAuditor_system() {
        // GIVEN
        var auth = new SystemUserAuthentication();
        // WHEN
        var auditor = getCurrentAuditor(auth);
        // THEN
        assertTrue(auditor.isSystem());
        assertEquals("SYSTEM", auditor.auditUserId());
    }

    @Test
    void getCurrentAuditor_unauthenticated() {
        // GIVEN
        var auth = new JeapAuthenticationToken(jwtApiGw(), emptySet(), emptyMap(), emptyList());
        auth.setAuthenticated(false);
        // WHEN / THEN
        assertThrows(AuditorProvider.UserNotAuthenticatedException.class, () -> getCurrentAuditor(auth));
    }

    @Test
    void getCurrentAuditor_unknownAuthentication() {
        // GIVEN
        var auth = new JwtAuthenticationToken(jwt(Map.of("randomClain", "--")), emptySet());
        auth.setAuthenticated(true);
        // WHEN / THEN
        assertThrows(AuditorProvider.UnknownAuthenticationException.class, () -> getCurrentAuditor(auth));
    }

    private static Jwt jwtEportal() {
        return jwt(
            Map.of(
                "iss",
                "https://auth.trust-infra.swiyu-int.admin.ch/realms/bj-swiyu-ecosystem",
                "sub",
                "06CB2415-8F57-4804-735B-DA1120B62C89",
                "given_name",
                "Max",
                "family_name",
                "Mustermann"
            )
        );
    }

    private static Jwt jwtAnonymous() {
        return jwt(Map.of("locale", "de"));
    }

    private static Jwt jwtApiGw() {
        return jwt(
            Map.of(
                "iss",
                "https://keymanager-npr.api.admin.ch/keycloak/realms/APIGW",
                "sub",
                "test-app_fbc9d4eb-cd87-4486-9e5a-08157940fbe0",
                "subscribedAPIs",
                emptyList()
            )
        );
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(100), Map.of("alg", "RS256"), claims);
    }
}
