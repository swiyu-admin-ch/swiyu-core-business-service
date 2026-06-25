package ch.admin.bj.swiyu.registry.status.service;

import static ch.admin.bj.swiyu.registry.status.api.DatastoreStatusDto.ACTIVE;
import static ch.admin.bj.swiyu.registry.status.api.DatastoreStatusDto.SETUP;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.registry.status.StatusRegistryTestConfig;
import ch.admin.bj.swiyu.registry.status.domain.StatusListDatastoreEntityRepository;
import ch.admin.bj.swiyu.registry.status.domain.VcEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = StatusRegistryTestConfig.Initializer.class)
@DataJpaTest
@Import({ StatusRegistryTestConfig.class, StatusListRegistryService.class })
class StatusListRegistryServiceIT {

    @Autowired
    private StatusListRegistryService statusListRegistryService;

    @Autowired
    private StatusListDatastoreEntityRepository statusListDatastoreEntityRepository;

    @Autowired
    private VcEntityRepository vcEntityRepository;

    @Autowired
    private StatusListDatastoreEntityRepository datastoreEntityRepository;

    @BeforeEach
    void setUp() {
        vcEntityRepository.deleteAllInBatch();
        statusListDatastoreEntityRepository.deleteAllInBatch();
    }

    @Test
    void createDatastoreEntry() {
        // WHEN
        var entity = this.statusListRegistryService.createDatastoreEntry();
        // THEN
        assertThat(entity.id()).isNotNull();
        assertThat(entity.status()).isEqualTo(SETUP);
    }

    @Test
    void publishStatusList() {
        // GIVEN
        var tokenStatusListJWT =
            "HEADER.ewogICJleHAiOiAyMjkxNzIwMTcwLAogICJpYXQiOiAxNjg2OTIwMTcwLAogICJpc3MiOiAiZGlkOnRkdzowMDAwOmV4YW1wbGU6IiwKICAic3RhdHVzX2xpc3QiOiB7CiAgICAiYml0cyI6IDEsCiAgICAibHN0IjogImVOcmJ1UmdBQWhjQlhRIgogIH0sCiAgInN1YiI6ICJodHRwczovL2V4YW1wbGUuY29tL3N0YXR1c2xpc3RzLzEuand0Igp9.SIGNATURE";
        var id = this.statusListRegistryService.createDatastoreEntry().id();
        // WHEN
        var entity = this.statusListRegistryService.publishStatusList(id, tokenStatusListJWT);
        // THEN
        assertThat(entity.id()).isEqualTo(id);
        assertThat(entity.status()).isEqualTo(ACTIVE);
        assertThat(entity.files().get("TokenStatusListJWT").isConfigured()).isTrue();
        assertThat(entity.files().get("TokenStatusListJWT").readUri()).isEqualTo("TEST.DATAURL/%s.jwt".formatted(id));
    }
}
