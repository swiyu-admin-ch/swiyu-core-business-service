package ch.admin.bj.swiyu.core.business.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WithAllTestContainerInitializers
@AutoConfigureMockMvc
class MessagingSecurityContextIntegrationIT {

    @Autowired
    private MessagingSecurityContext messagingSecurityContext;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setPreferredUser_withSystemAndService_setsExpectedSecurityContext() {
        // given
        AvroDomainEventPublisher publisher = mock(AvroDomainEventPublisher.class);
        when(publisher.getOptionalSystem()).thenReturn(Optional.of("foo"));
        when(publisher.getOptionalService()).thenReturn(Optional.of("bar"));

        // when
        messagingSecurityContext.setPreferredUser(publisher);

        // then
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(JeapAuthenticationToken.class);
        assertThat(((JeapAuthenticationToken) authentication).getPreferredUsername()).isEqualTo("foo_bar");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_SYSTEM");
    }

    @Test
    void setPreferredUser_withEmptyOptionals_setsAnonymousUser() {
        // given
        AvroDomainEventPublisher publisher = mock(AvroDomainEventPublisher.class);
        when(publisher.getOptionalSystem()).thenReturn(Optional.empty());
        when(publisher.getOptionalService()).thenReturn(Optional.empty());

        // when
        messagingSecurityContext.setPreferredUser(publisher);

        // then
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(JeapAuthenticationToken.class);
        assertThat(((JeapAuthenticationToken) authentication).getPreferredUsername()).isEqualTo("anonymous_anonymous");
    }
}
