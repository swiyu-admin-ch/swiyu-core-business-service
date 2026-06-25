package ch.admin.bj.swiyu.registry.identifier.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryTestConfig;
import ch.admin.bj.swiyu.registry.identifier.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.registry.identifier.domain.DatastoreStatus;
import ch.admin.bj.swiyu.registry.identifier.domain.DidEntityRepository;
import ch.admin.bj.swiyu.registry.identifier.domain.DidType;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntityRepository;
import ch.admin.bj.swiyu.registry.identifier.test.IdentifierRegistryTestData;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = IdentifierRegistryTestConfig.Initializer.class)
@DataJpaTest
@Import({ IdentifierRegistryTestConfig.class, IdentifierRegistryService.class })
class IdentifierRegistryServiceIT {

    @Autowired
    private IdentifierRegistryService identifierRegistryService;

    @Autowired
    private IdentifierDatastoreEntityRepository identifierDatastoreEntityRepository;

    @Autowired
    private DidEntityRepository didEntityRepository;

    @Autowired
    @Qualifier("identifierRegistryTransactionManager")
    private PlatformTransactionManager identifierRegistryTransactionManager;

    @BeforeEach
    void setUp() {
        didEntityRepository.deleteAllInBatch();
        identifierDatastoreEntityRepository.deleteAllInBatch();
    }

    @Test
    void createDatastoreEntity() {
        // WHEN
        var entity = identifierRegistryService.createDatastoreEntity();
        // THEN
        assertThat(entity).isNotNull();
    }

    @Test
    void getDatastoreEntity() {
        // GIVEN
        var entity = identifierDatastoreEntityRepository.save(
            IdentifierRegistryTestData.datastoreEntity(DatastoreStatus.SETUP)
        );
        // WHEN
        var dto = identifierRegistryService.getDatastoreEntity(entity.getId());
        // THEN
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(entity.getId());
        assertThat(dto.status()).isEqualTo(DatastoreStatusDto.SETUP);
        assertThat(dto.files()).hasSize(2);
    }

    @Test
    void updateDidTdwEntry() {
        // GIVEN
        var entity = identifierDatastoreEntityRepository.save(
            IdentifierRegistryTestData.datastoreEntity(DatastoreStatus.SETUP)
        );
        // WHEN
        var dto = identifierRegistryService.updateDidTdwEntry(entity.getId(), "dummy_json_log");
        // THEN
        assertThat(dto.id()).isEqualTo(entity.getId());
        assertThat(dto.status()).isEqualTo(DatastoreStatusDto.ACTIVE);
        var did = didEntityRepository.findByBase_IdAndFileType(entity.getId(), DidType.DID_TDW);
        assertThat(did).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void updateDidTdwEntry_concurrentCalls_throwsConcurrentModificationOrSucceeds() throws Exception {
        // GIVEN - commit entity so both threads can see it (no outer test transaction)
        var txTemplate = new TransactionTemplate(identifierRegistryTransactionManager);
        var entityId = txTemplate.execute(status ->
            identifierDatastoreEntityRepository
                .save(IdentifierRegistryTestData.datastoreEntity(DatastoreStatus.SETUP))
                .getId()
        );

        // WHEN - two threads released simultaneously to trigger the race window
        var exceptions = new ConcurrentLinkedQueue<Throwable>();
        var latch = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        var f1 = CompletableFuture.runAsync(
            () -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    identifierRegistryService.updateDidTdwEntry(entityId, "content_1");
                } catch (DataIntegrityViolationException e) {
                    exceptions.add(e);
                }
            },
            executor
        );
        var f2 = CompletableFuture.runAsync(
            () -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    identifierRegistryService.updateDidTdwEntry(entityId, "content_2");
                } catch (DataIntegrityViolationException e) {
                    exceptions.add(e);
                }
            },
            executor
        );

        latch.countDown();
        CompletableFuture.allOf(f1, f2).get(10, TimeUnit.SECONDS);

        // THEN - any conflict is surfaced as DataIntegrityViolationException (not a raw 500)
        assertThat(exceptions).allSatisfy(t -> assertThat(t).isInstanceOf(DataIntegrityViolationException.class));
        // exactly one did_entity row regardless of whether a conflict occurred
        assertThat(didEntityRepository.findByBase_Id(entityId)).hasSize(1);

        executor.shutdown();
    }
}
