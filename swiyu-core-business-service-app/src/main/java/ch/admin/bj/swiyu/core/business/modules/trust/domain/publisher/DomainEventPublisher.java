package ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher;

import static net.logstash.logback.argument.StructuredArguments.kv;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionSubmittedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.common.BeanReferenceMessageKey;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Slf4j
public class DomainEventPublisher {

    public static final String TOPIC = "topic";

    private final TransactionalOutbox outbox;

    // Saving events to the outbox must occur in the same transaction as the database operation
    // itself, otherwise it's quite pointless to use a transactional outbox.
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishVcSchemaSubmissionAcceptedEvent(@NonNull TiVcSchemaSubmissionAcceptedEvent event) {
        var topicName = TiVcSchemaSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiVcSchemaSubmissionAcceptedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getVcSchemaSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTiTrustAddDidSubmissionSubmittedEvent(@NonNull TiTrustAddDidSubmissionSubmittedEvent event) {
        var topicName = TiTrustAddDidSubmissionSubmittedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiTrustAddDidSubmissionSubmittedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getTrustAddDidSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTiTrustOnboardingSubmissionAcceptedEvent(
        @NonNull TiTrustOnboardingSubmissionAcceptedEvent event
    ) {
        var topicName = TiTrustOnboardingSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiTrustOnboardingSubmissionAcceptedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getTrustOnboardingSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishVqpsSubmissionAcceptedEvent(@NonNull TiVqpsSubmissionAcceptedEvent event) {
        var topicName = TiVqpsSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiVqpsSubmissionAcceptedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getVqpsSubmissionId().toString())
                .build(),
            event
        );
    }

    private void sendEvent(final String topicName, Object key, final AvroMessage event) {
        log.info("Publishing to topic {}", kv(TOPIC, topicName));
        outbox.sendMessage(event, key, topicName);
    }
}
