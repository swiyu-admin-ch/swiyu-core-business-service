package ch.admin.bj.swiyu.core.business.test;

import static org.mockito.Mockito.mock;

import ch.admin.bit.jeap.audit.command.builder.spring.CreateAuditCommandAutoConfiguration;
import ch.admin.bit.jeap.audit.transactional.outbox.spring.AuditTransactionalOutboxAutoConfiguration;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.transactionaloutbox.config.TransactionalOutboxConfigurationProperties;
import ch.admin.bit.jeap.messaging.transactionaloutbox.jpa.OutboxJpaConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxConfig;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxTracing;
import ch.admin.bit.jeap.messaging.transactionaloutbox.transaction.OutboxTransactionConfig;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import org.apache.avro.generic.GenericData;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

@Import(
    {
        OutboxConfig.class,
        OutboxJpaConfig.class,
        OutboxTransactionConfig.class,
        TransactionalOutboxConfigurationProperties.class,
        // Audit Publishing
        AuditPublisher.class,
        CreateAuditCommandAutoConfiguration.class,
        AuditTransactionalOutboxAutoConfiguration.class,
    }
)
@EnableConfigurationProperties(KafkaProperties.class)
public class DataJpaTestKafkaConfiguration {

    @Bean
    public DefaultKafkaProducerFactory<?, ?> kafkaProducerFactory() {
        return mock(DefaultKafkaProducerFactory.class);
    }

    @Bean
    public OutboxTracing outboxTracing() {
        return mock(OutboxTracing.class);
    }

    @Bean
    public KafkaAvroSerdeProvider kafkaAvroSerdeProvider() {
        return new KafkaAvroSerdeProvider(
            (s, o) -> new byte[0],
            (s, o) -> new byte[0],
            (s, bytes) -> mock(GenericData.Record.class),
            mock(KafkaAvroSerdeProperties.class)
        );
    }

    @Bean
    public ContractsValidator contractsValidator() {
        return mock(ContractsValidator.class);
    }
}
