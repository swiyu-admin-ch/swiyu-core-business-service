package ch.admin.bj.swiyu.core.business.common.security;

import ch.admin.bit.jeap.messaging.model.MessagePublisher;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class MessagingSecurityContext {

    /**
     * Takes {@link MessagePublisher}, the interface shared by events and commands, so the same call
     * works for both - {@code AvroDomainEventPublisher} and {@code AvroMessagePublisher} are unrelated
     * generated classes that only meet in this interface.
     */
    public void setPreferredUser(MessagePublisher publisher) {
        // Use the publisher's topic name as the preferred username
        String systemName = publisher.getSystem() == null ? "anonymous" : publisher.getSystem();
        String serviceName = publisher.getService() == null ? "anonymous" : publisher.getService();
        setPreferredUser("%s_%s".formatted(systemName, serviceName));
    }

    /**
     * Set security context so that audit content still can be filled in case the entity change is triggered by
     * a kafka event and not an actual user request.
     * @param preferredUsername
     */
    private void setPreferredUser(String preferredUsername) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("preferred_username", preferredUsername);
        claims.put("clientId", "system-client");
        claims.put("iss", "system-issuer");
        claims.put("sub", "system-subject");
        claims.put("given_name", "System");
        claims.put("family_name", "User");
        claims.put("name", "System User");
        claims.put("locale", "de");

        // You can also add ext_id or admin_dir_uid if needed

        Jwt jwt = new Jwt(
            "dummy-token-value", // token value
            Instant.now(), // issued at
            Instant.now().plusSeconds(3600), // expires at
            Map.of("alg", "none"), // headers
            claims // claims
        );

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"));
        var userRoles = Set.<String>of(); // if needed
        var bproles = Map.<String, Set<String>>of(); // or add some mock bproles if you want

        var auth = new JeapAuthenticationToken(jwt, userRoles, bproles, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
