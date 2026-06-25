package ch.admin.bj.swiyu.core.business.common.audit;

import static ch.admin.bj.swiyu.core.business.common.audit.AuditorProvider.getCurrentAuditor;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing
@Slf4j
public class AuditConfig {

    /**
     * Provides the current auditor (user) for JPA auditing. It retrieves the current authentication from the security context and determines the auditor based on the type of authentication. It also logs if data is being mutated in a system context or by an anonymous user, which should not happen. The auditor is returned as an Optional string, which can be used by JPA auditing to populate audit fields like createdBy and lastModifiedBy.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            var auditor = getCurrentAuditor(SecurityContextHolder.getContext().getAuthentication());
            if (auditor.isSystem()) {
                log.trace("Data is being mutated in system context.");
            } else if (auditor.isAnonymous()) {
                log.error("Data is being mutated by an anonymous user. This should not happen.");
            }
            return Optional.of(auditor.auditUserId());
        };
    }
}
