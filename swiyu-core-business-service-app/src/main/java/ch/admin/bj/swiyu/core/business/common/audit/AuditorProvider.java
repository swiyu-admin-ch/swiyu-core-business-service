package ch.admin.bj.swiyu.core.business.common.audit;

import static ch.admin.bj.swiyu.core.business.common.audit.AuditorProvider.AuditorType.*;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.security.SystemUserAuthentication;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

/**
 * Resolves the current auditor for auditing purposes. The auditor is determined based on the type of authentication:
 * <ul>
 *     <li><strong>System User:</strong> SYSTEM</li>
 *     <li><strong>Anonymous User:</strong> ANONYMOUS</li>
 *     <li><strong>Authenticated User:</strong> {subject}:{family_name},{given_name}</li>
 *     <li><strong>B2B User:</strong> {subject}:system_user_of_business_partner</li>
 * <ul/>
 */
@Slf4j
@UtilityClass
public class AuditorProvider {

    /**
     * Resolves the current auditor based on the provided authentication. The method checks the type of authentication and extracts the relevant information to create an Auditor instance. If the authentication is not recognized or if an unauthenticated user tries to perform an action, appropriate exceptions are thrown to prevent data corruption.
     */
    public static Auditor getCurrentAuditor(Authentication auth) {
        if (auth instanceof SystemUserAuthentication) {
            return new Auditor(SYSTEM.toString(), SYSTEM, "none");
        } else if (auth instanceof JeapAuthenticationToken jeapAuth) {
            if (!jeapAuth.isAuthenticated()) {
                // No changes allowed without authentication
                throw new UserNotAuthenticatedException();
            }
            if (jeapAuth.getTokenSubject() == null) {
                // This is a special case for test execution, because @WithJeapAuthenticationToken doesn't set the subject
                return new Auditor(ANONYMOUS.toString(), AuditorType.ANONYMOUS, "none");
            }
            var subject = jeapAuth.getTokenSubject();
            var identityProvider =
                jeapAuth.getTokenAttributes().get("iss") != null
                    ? jeapAuth.getTokenAttributes().get("iss").toString()
                    : null;

            var isApiGw = jeapAuth.getTokenAttributes().containsKey("subscribedAPIs");
            if (isApiGw) {
                var id = "%s:system_user_of_business_partner".formatted(subject);
                return new Auditor(id, AuditorType.USER, identityProvider);
            } else {
                var firstname = jeapAuth.getTokenGivenName();
                var lastname = jeapAuth.getTokenFamilyName();
                var id = "%s:%s,%s".formatted(subject, lastname, firstname);
                return new Auditor(id, AuditorType.USER, identityProvider);
            }
        } else {
            throw new UnknownAuthenticationException();
        }
    }

    /**
     * Type of auditor, which can be either a system user, an authenticated user or an anonymous user.
     */
    public enum AuditorType {
        SYSTEM,
        USER,
        ANONYMOUS,
    }

    /**
     * Represents the auditor performing an action. Contains the audit user id, the type of auditor and the identity provider (if applicable).
     */
    public record Auditor(@NotNull String auditUserId, @NotNull AuditorType type, String identityProvider) {
        public boolean isSystem() {
            return type == SYSTEM;
        }

        public boolean isAnonymous() {
            return type == ANONYMOUS;
        }
    }

    /**
     * Exception thrown when an unauthenticated user tries to perform an action that requires authentication. This should not happen, because the security configuration should prevent this. However, this is a safety net to prevent data corruption in case of misconfiguration.
     */
    public class UserNotAuthenticatedException extends RuntimeException {

        UserNotAuthenticatedException() {
            super("Data is being mutated by an unauthenticated user. This should not happen.");
        }
    }

    /**
     * Exception thrown when an unknown authentication mechanism is used. This should not happen, because the security configuration should prevent this. However, this is a safety net to prevent data corruption in case of misconfiguration.
     */
    public class UnknownAuthenticationException extends RuntimeException {

        UnknownAuthenticationException() {
            super("Data is being mutated by an unknown security mechanism. This should not happen.");
        }
    }
}
