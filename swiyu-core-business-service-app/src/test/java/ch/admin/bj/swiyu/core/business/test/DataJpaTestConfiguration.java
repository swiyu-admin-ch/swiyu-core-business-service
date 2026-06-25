package ch.admin.bj.swiyu.core.business.test;

import static org.mockito.Mockito.mock;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import ch.admin.bit.jeap.starter.db.config.FlywayMigrationConfiguration;
import ch.admin.bj.swiyu.core.business.common.persistence.CorePersistenceConfig;
import ch.admin.bj.swiyu.core.business.modules.identifier.config.IdentifierLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.MockPamsClient;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListsLimitProperties;
import java.util.Optional;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Holds all necessary configurations for sliced @DataJpaTest integration tests which are common to all.
 */
@Import({ TestRepositories.class, FlywayMigrationConfiguration.class, CorePersistenceConfig.class })
@AutoConfigureObservability // since audit publisher uses trace-id
@EnableJpaAuditing
@AutoConfigureJson
@EnableConfigurationProperties(
    { FlywayProperties.class, IdentifierLimitProperties.class, StatusListsLimitProperties.class }
)
public class DataJpaTestConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("TestUser");
    }

    @Bean
    public PamsClient pamsClient() {
        return new MockPamsClient();
    }

    @Bean // required bean when using jeaps AuditTransactionalOutboxAutoConfiguration in jpa tests
    public ServletSemanticAuthorization servletSemanticAuthorization() {
        return mock(ServletSemanticAuthorization.class);
    }

    @Bean
    public ThreadPoolTaskExecutor defaultSpringBootAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        return executor;
    }
}
