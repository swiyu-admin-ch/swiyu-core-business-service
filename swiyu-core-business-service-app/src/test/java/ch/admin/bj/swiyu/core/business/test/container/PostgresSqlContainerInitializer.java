package ch.admin.bj.swiyu.core.business.test.container;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class PostgresSqlContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static PostgreSQLContainer<?> coreDatabase;
    private static PostgreSQLContainer<?> registryIdentifierDatabase;
    private static PostgreSQLContainer<?> statusIdentifierDatabase;

    private static PostgreSQLContainer<?> getCoreDatabase() {
        if (coreDatabase == null) {
            coreDatabase = createPostgresTestContainer();
        }
        return coreDatabase;
    }

    private static PostgreSQLContainer<?> getRegistryIdentifierDatabase() {
        if (registryIdentifierDatabase == null) {
            registryIdentifierDatabase = createPostgresTestContainer();
        }
        return registryIdentifierDatabase;
    }

    private static PostgreSQLContainer<?> getRegistryStatusDatabase() {
        if (statusIdentifierDatabase == null) {
            statusIdentifierDatabase = createPostgresTestContainer();
        }
        return statusIdentifierDatabase;
    }

    private static PostgreSQLContainer<?> createPostgresTestContainer() {
        var container = new PostgreSQLContainer<>(
            DockerImageName.parse("docker-hub.nexus.bit.admin.ch/postgres:17.8").asCompatibleSubstituteFor(
                "postgres:17.8"
            )
        );
        container.start();
        log.info("PostgreSQL container started at: {}", container.getJdbcUrl());
        return container;
    }

    public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "spring.datasource.core-db.url=" + getCoreDatabase().getJdbcUrl(),
            "spring.datasource.core-db.username=" + getCoreDatabase().getUsername(),
            "spring.datasource.core-db.password=" + getCoreDatabase().getPassword()
        );
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "spring.datasource.identifier-registry-db.url=" + getRegistryIdentifierDatabase().getJdbcUrl(),
            "spring.datasource.identifier-registry-db.username=" + getRegistryIdentifierDatabase().getUsername(),
            "spring.datasource.identifier-registry-db.password=" + getRegistryIdentifierDatabase().getPassword()
        );
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "spring.datasource.status-registry-db.url=" + getRegistryStatusDatabase().getJdbcUrl(),
            "spring.datasource.status-registry-db.username=" + getRegistryStatusDatabase().getUsername(),
            "spring.datasource.status-registry-db.password=" + getRegistryStatusDatabase().getPassword()
        );
    }
}
