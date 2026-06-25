package ch.admin.bj.swiyu.core.business.common.persistence;

import ch.admin.bit.jeap.starter.db.config.DatabaseMigrationProperties;
import ch.admin.bit.jeap.starter.db.config.FlywayMigrationStrategyResolver;
import ch.admin.bit.jeap.starter.db.config.ShutdownService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MultiDbMigrationConfiguration {

    private final ShutdownService shutdownService;
    private final ApplicationContext ctx;
    private final Environment environment;
    private final FlywayMigrationStrategyResolver flywayMigrationStrategyResolver;
    private final DatabaseMigrationProperties databaseMigrationProperties;

    @Value("${database-migration.expected-count-of-flyway-migrations}")
    int expectedCountOfFlywayMigrations = 1;

    @Bean
    @Primary
    public FlywayMigrationStrategy overridingFlywayMigrationStrategy() {
        return currentFlyway -> {
            try {
                flywayMigrationStrategyResolver.doResolveFlywayStrategy(ctx, environment, currentFlyway);
            } catch (Exception e) {
                log.error("An error occurred during Flyway migration strategy execution: {}", e.getMessage(), e);
                if (this.doShutdown()) {
                    this.shutdownService.shutdown(ctx, 1);
                    return;
                }

                throw e;
            }

            if (this.doShutdown()) {
                var flyways = ctx.getBeansOfType(Flyway.class).entrySet();
                var currentCount = flyways.size() + 1;
                if (currentCount < expectedCountOfFlywayMigrations) {
                    log.info(
                        "Waiting for Flyway migrations to finish before shutting down. {}/{} done.",
                        currentCount,
                        expectedCountOfFlywayMigrations
                    );
                } else if (currentCount == expectedCountOfFlywayMigrations) {
                    log.info("Flyway migrations did finish. Shutting down.");
                    this.shutdownService.shutdown(ctx, 0);
                }
            }
        };
    }

    private boolean doShutdown() {
        return (
            this.databaseMigrationProperties.isInitContainer() &&
            !this.databaseMigrationProperties.isStartupMigrateModeEnabled()
        );
    }
}
