package ch.admin.bj.swiyu.core.business.common.persistence;

import static ch.admin.bj.swiyu.core.business.common.persistence.CorePersistenceConfig.*;

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
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This configuration initializes the connection and flyway migration to the core database.
 * <p>
 * Note: As of now the trust management service connects to 3 databases. Once the replication is handled properly
 * everything will be merged into the core database with dedicated schemas.
 */
@Slf4j
@AllArgsConstructor
@Configuration
@EnableJpaRepositories(basePackages = PACKAGES_TO_SCAN)
public class CorePersistenceConfig {

    protected static final String PACKAGES_TO_SCAN = "ch.admin.bj.swiyu.core.business";
    private static final String CORE_DB_FLYWAY_LOCATION = "classpath:db/migration/core";

    /**
     * Flyway locations needed in order to know if additional flyway scripts shall be executed
     * besides default "db/migration/core". This is needed for custom after migrate scripts (e.g. fixing
     * db permissions).
     */
    @Value("${spring.flyway.locations:[]}")
    private final List<String> additionalFlywayLocations = new ArrayList<>();

    private final JpaProperties jpaProperties;
    private final FlywayMigrationStrategy migrationStrategy;

    @Bean
    @ConfigurationProperties("spring.datasource.core-db")
    public DataSourceProperties coreDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig globalHikariConfig() {
        return new HikariConfig();
    }

    @Primary
    @Bean(name = "coreDataSource")
    public DataSource coreDataSource(DataSourceProperties coreDataSourceProperties, HikariConfig globalHikariConfig) {
        var config = new HikariConfig();
        config.setMaximumPoolSize(globalHikariConfig.getMaximumPoolSize());
        config.setJdbcUrl(coreDataSourceProperties.getUrl());
        config.setUsername(coreDataSourceProperties.getUsername());
        config.setPassword(coreDataSourceProperties.getPassword());
        config.setDriverClassName(coreDataSourceProperties.getDriverClassName());
        config.setSchema(gerDefaultSchema());
        config.setPoolName("connection-pool-core-db");
        return new HikariDataSource(config);
    }

    @Primary // Required by transaction outbox because it relies on a default entityManagerFactory
    @Bean
    @DependsOn("coreFlyway")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
        @Qualifier("coreDataSource") DataSource datasource
    ) {
        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(datasource);
        em.setPackagesToScan(PACKAGES_TO_SCAN, "ch.admin.bit.jeap.messaging");
        var vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaPropertyMap(jpaProperties.getProperties());
        return em;
    }

    @Primary // Required by transaction outbox because it relies on a default transaction manager
    @Bean
    public PlatformTransactionManager transactionManager(
        @Qualifier("coreDataSource") DataSource datasource,
        LocalContainerEntityManagerFactoryBean entityManagerFactory
    ) {
        var manager = new JpaTransactionManager();
        manager.setJpaPropertyMap(jpaProperties.getProperties());
        manager.setDataSource(datasource);
        manager.setEntityManagerFactory(entityManagerFactory.getObject());
        return manager;
    }

    @Bean
    public Flyway coreFlyway(@Qualifier("coreDataSource") DataSource datasource) {
        log.debug("Migrating database {} ...", CORE_DB_FLYWAY_LOCATION);
        var locations = new ArrayList<>(List.of(CORE_DB_FLYWAY_LOCATION));
        if (!additionalFlywayLocations.isEmpty()) {
            log.debug("Adding additional flyway locations from properties: {}", additionalFlywayLocations);
            locations.addAll(additionalFlywayLocations);
        }
        var flyway = Flyway.configure()
            .dataSource(datasource)
            .defaultSchema(gerDefaultSchema())
            .locations(locations.toArray(new String[0]))
            .load();
        migrationStrategy.migrate(flyway);
        return flyway;
    }

    private String gerDefaultSchema() {
        return jpaProperties.getProperties().get("hibernate.default_schema");
    }
}
