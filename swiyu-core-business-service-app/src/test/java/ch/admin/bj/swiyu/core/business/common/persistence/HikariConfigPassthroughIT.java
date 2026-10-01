package ch.admin.bj.swiyu.core.business.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that the whole {@code spring.datasource.hikari} configuration is passed through to each
 * HikariDataSource
 */
@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(
    properties = { "spring.datasource.hikari.connection-timeout=2000", "spring.datasource.hikari.maximum-pool-size=3" }
)
@WithAllTestContainerInitializers
class HikariConfigPassthroughIT {

    private static final int POOL_SIZE = 3;

    @Autowired
    @Qualifier("coreDataSource")
    private DataSource coreDataSource;

    @Autowired
    @Qualifier("statusRegistryDataSource")
    private DataSource statusRegistryDataSource;

    @Autowired
    @Qualifier("identifierRegistryDataSource")
    private DataSource identifierRegistryDataSource;

    @Test
    void coreDataSourceHonorsAllHikariSettings() {
        var hds = (HikariDataSource) coreDataSource;
        assertThat(hds.getConnectionTimeout()).isEqualTo(2000L);
        assertThat(hds.getMaximumPoolSize()).isEqualTo(POOL_SIZE);
    }

    @Test
    void statusRegistryDataSourceHonorsAllHikariSettings() {
        var hds = (HikariDataSource) statusRegistryDataSource;
        assertThat(hds.getConnectionTimeout()).isEqualTo(2000L);
        assertThat(hds.getMaximumPoolSize()).isEqualTo(POOL_SIZE);
    }

    @Test
    void identifierRegistryDataSourceHonorsAllHikariSettings() {
        var hds = (HikariDataSource) identifierRegistryDataSource;
        assertThat(hds.getConnectionTimeout()).isEqualTo(2000L);
        assertThat(hds.getMaximumPoolSize()).isEqualTo(POOL_SIZE);
    }
}
