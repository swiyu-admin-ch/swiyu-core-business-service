package ch.admin.bj.swiyu.registry.status.common.persistence;

import static ch.admin.bj.swiyu.registry.status.common.persistence.StatusRegistryPersistenceConfig.*;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This configuration initializes the connection and flyway migration to the public status (base) registry database.
 * <p>
 * Note: As of now the swiyu-core-business-service connects to 3 databases. Once the replication is handled properly
 * everything will be merged into the core database with dedicated schemas.
 */
@Slf4j
@AllArgsConstructor
@Configuration
@EnableJpaRepositories(
    basePackages = PACKAGES_TO_SCAN,
    entityManagerFactoryRef = TRANSACTION_MANAGER_FACTORY,
    transactionManagerRef = TRANSACTION_MANAGER
)
public class StatusRegistryPersistenceConfig {

    protected static final String TRANSACTION_MANAGER = "statusRegistryTransactionManager";
    protected static final String TRANSACTION_MANAGER_FACTORY = "statusRegistryEntityManagerFactory";
    protected static final String PACKAGES_TO_SCAN = "ch.admin.bj.swiyu.registry.status";
    private static final String STATUS_REGISTRY_DB_FLYWAY_LOCATION = "classpath:db/migration/status-registry";

    /**
     * Flyway locations needed in order to know if additional flyway scripts shall be executed
     * besides default "db/migration/core". This is needed for custom after migrate scripts (e.g. fixing
     * db permissions).
     */
    @Value("${spring.flyway.locations:[]}")
    private final List<String> addtionalFlywayLocations = new ArrayList<>();

    private final FlywayMigrationStrategy migrationStrategy;
    private final JpaProperties jpaProperties;

    @Bean
    @ConfigurationProperties("spring.datasource.status-registry-db")
    public DataSourceProperties statusRegistryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "statusRegistryDataSource")
    public DataSource statusRegistryDataSource(
        DataSourceProperties statusRegistryDataSourceProperties,
        HikariConfig globalHikariConfig
    ) {
        var config = new HikariConfig();
        config.setMaximumPoolSize(globalHikariConfig.getMaximumPoolSize());
        config.setJdbcUrl(statusRegistryDataSourceProperties.getUrl());
        config.setUsername(statusRegistryDataSourceProperties.getUsername());
        config.setPassword(statusRegistryDataSourceProperties.getPassword());
        config.setDriverClassName(statusRegistryDataSourceProperties.getDriverClassName());
        config.setSchema(getDefaultSchema());
        config.setPoolName("connection-pool-status-registry-db");
        return new HikariDataSource(config);
    }

    @Bean("statusRegistryEntityManagerFactory")
    @DependsOn("statusRegistryFlyway")
    public LocalContainerEntityManagerFactoryBean statusRegistryEntityManagerFactory(
        @Qualifier("statusRegistryDataSource") DataSource datasource
    ) {
        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(datasource);
        em.setPackagesToScan(PACKAGES_TO_SCAN);
        var vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaPropertyMap(jpaProperties.getProperties());
        return em;
    }

    @Bean(TRANSACTION_MANAGER)
    public PlatformTransactionManager statusRegistryTransactionManager(
        @Qualifier("statusRegistryDataSource") DataSource datasource,
        @Qualifier(TRANSACTION_MANAGER_FACTORY) LocalContainerEntityManagerFactoryBean factory
    ) {
        var manager = new JpaTransactionManager();
        manager.setJpaPropertyMap(jpaProperties.getProperties());
        manager.setDataSource(datasource);
        manager.setEntityManagerFactory(factory.getObject());
        return manager;
    }

    @Bean
    public Flyway statusRegistryFlyway(@Qualifier("statusRegistryDataSource") DataSource datasource) {
        log.debug("Migrating database {} ...", STATUS_REGISTRY_DB_FLYWAY_LOCATION);
        var locations = new ArrayList<>(List.of(STATUS_REGISTRY_DB_FLYWAY_LOCATION));
        if (!addtionalFlywayLocations.isEmpty()) {
            log.debug("Adding additional flyway locations from properties: {}", addtionalFlywayLocations);
            locations.addAll(addtionalFlywayLocations);
        }
        var flyway = Flyway.configure()
            .dataSource(datasource)
            .defaultSchema(getDefaultSchema())
            .locations(locations.toArray(new String[0]))
            .load();
        migrationStrategy.migrate(flyway);
        return flyway;
    }

    private String getDefaultSchema() {
        return jpaProperties.getProperties().get("hibernate.default_schema");
    }
}
