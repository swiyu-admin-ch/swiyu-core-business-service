package ch.admin.bj.swiyu.core.business.common.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

class AuditConfigTest {

    private final AuditConfig auditConfig = new AuditConfig();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAuditorProvider_authenticated() {
        JeapAuthenticationToken token = mock(JeapAuthenticationToken.class);
        when(token.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.setContext(new SecurityContextImpl(token));

        // Act
        Optional<String> auditor = auditConfig.auditorProvider().getCurrentAuditor();

        // Assert
        assertTrue(auditor.isPresent());
        assertEquals("ANONYMOUS", auditor.get());
    }

    @Test
    void testAuditorProvider_unauthenticated() {
        // Arrange
        JeapAuthenticationToken token = mock(JeapAuthenticationToken.class);
        when(token.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.setContext(new SecurityContextImpl(token));

        // Act / Assert
        assertThrows(AuditorProvider.UserNotAuthenticatedException.class, () ->
            auditConfig.auditorProvider().getCurrentAuditor()
        );
    }
}
