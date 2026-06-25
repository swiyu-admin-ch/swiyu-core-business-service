package ch.admin.bj.swiyu.registry.status;

import ch.admin.bit.jeap.starter.db.config.FlywayMigrationConfiguration;
import com.zaxxer.hikari.HikariConfig;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootConfiguration // needed since we do not have a main application class in this library
@EnableAutoConfiguration // needed since we do not have a main application class in this library
@Import({ StatusRegistryConfig.class, FlywayMigrationConfiguration.class })
@EnableJpaAuditing
@EnableConfigurationProperties({ FlywayProperties.class })
@Testcontainers
public class StatusRegistryTestConfig {

    @Container
    static PostgreSQLContainer<?> database = new PostgreSQLContainer<>(
        DockerImageName.parse("docker-hub.nexus.bit.admin.ch/postgres:17.8").asCompatibleSubstituteFor("postgres:17.8")
    );

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("TestUser");
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig globalHikariConfig() {
        return new HikariConfig();
    }

    /**
     * Sets-up all required spring properties and bootstraps the postgres testcontainer db.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
            database.start();
            // StatusRegistryProperties
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                configurableApplicationContext,
                "app.status-registry.data-url-template=TEST.DATAURL/{0}.{1}"
            );

            // General spring properties since we do not have an application-test.yml in this library
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                configurableApplicationContext,
                "spring.datasource.status-registry-db.driver-class-name=org.postgresql.Driver",
                "spring.datasource.status-registry-db.type=com.zaxxer.hikari.HikariDataSource",
                "spring.datasource.hikari.maximum-pool-size=2",
                "spring.jpa.open-in-view=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy",
                "spring.jpa.properties.hibernate.default_schema=data",
                "spring.flyway.enabled=true"
            );
            // set the database connection properties for the status registry database
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                configurableApplicationContext,
                "spring.datasource.status-registry-db.url=" + database.getJdbcUrl(),
                "spring.datasource.status-registry-db.username=" + database.getUsername(),
                "spring.datasource.status-registry-db.password=" + database.getPassword()
            );
            // database migration (execute flyway on startup)
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                configurableApplicationContext,
                "database-migration.startup-migrate-mode-enabled=true"
            );
        }
    }
}
