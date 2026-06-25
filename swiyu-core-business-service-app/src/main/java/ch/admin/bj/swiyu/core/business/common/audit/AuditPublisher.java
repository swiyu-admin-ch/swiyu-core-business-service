package ch.admin.bj.swiyu.core.business.common.audit;

import static ch.admin.bit.jeap.audit.record.create.AuditObjectDataRole.NEW;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditEventDataKey.BUSINESS_PARTNER_ID;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditEventDataKey.USE_CASE_CATEGORY_ID;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.BUSINESS_PARTNER_REGISTERED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.BUSINESS_PARTNER_UPDATED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.IDENTIFIER_ENTRY_CHANGED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.IDENTIFIER_ENTRY_CREATED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.IDENTIFIER_ENTRY_DESCRIPTION_CHANGED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.STATUS_LIST_CHANGED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.STATUS_LIST_CREATED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.TRUST_ONBOARDING_DOCUMENT_UPLOADED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.TRUST_ONBOARDING_SUBMITTED;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditorProvider.getCurrentAuditor;

import ch.admin.bit.jeap.audit.command.builder.CreateAuditRecordCommandBuilder;
import ch.admin.bit.jeap.audit.record.create.CreateAuditRecordCommand;
import ch.admin.bit.jeap.audit.transactional.outbox.CreateAuditRecordCommandTransactionOutboxSender;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Slf4j
@JeapMessageProducerContract(
    value = CreateAuditRecordCommand.TypeRef.class,
    topic = "ti-create-audit-record",
    encryptionKeyId = "messagingKey"
)
public class AuditPublisher {

    private static final String DEPARTMENT_NAME = "BJ";

    private final CreateAuditRecordCommandTransactionOutboxSender sender;
    private final KafkaProperties kafkaProperties;
    private final Tracer tracer;

    @Transactional(propagation = Propagation.MANDATORY)
    public void statusListEntryChanged(
        String statusListEntryId,
        String uploadCount,
        String businessPartnerId,
        String statusListEntryJson,
        String statusListVc
    ) {
        publishAuditEvent(
            STATUS_LIST_CHANGED,
            statusListEntryId,
            uploadCount,
            businessPartnerId,
            statusListEntryJson,
            statusListVc
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void statusListEntryCreated(String statusListEntryId, String businessPartnerId, String statusListEntryJson) {
        publishAuditEvent(STATUS_LIST_CREATED, statusListEntryId, "0", businessPartnerId, statusListEntryJson);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void identifierEntryCreated(String identifierEntryId, String businessPartnerId, String identifierEntryJson) {
        publishAuditEvent(IDENTIFIER_ENTRY_CREATED, identifierEntryId, "0", businessPartnerId, identifierEntryJson);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void identifierEntryChanged(
        String identifierEntryId,
        String uploadCount,
        String businessPartnerId,
        String identifierEntryJson,
        String didLog
    ) {
        publishAuditEvent(
            IDENTIFIER_ENTRY_CHANGED,
            identifierEntryId,
            uploadCount,
            businessPartnerId,
            identifierEntryJson,
            didLog
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void identifierEntryDescriptionChanged(
        String identifierEntryId,
        String uploadCount,
        String businessPartnerId,
        String identifierEntryJson
    ) {
        publishAuditEvent(
            IDENTIFIER_ENTRY_DESCRIPTION_CHANGED,
            identifierEntryId,
            uploadCount,
            businessPartnerId,
            identifierEntryJson
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void businessPartnerRegistered(String businessPartnerId, String version, String businessPartnerJson) {
        publishAuditEvent(
            BUSINESS_PARTNER_REGISTERED,
            businessPartnerId,
            version,
            businessPartnerId,
            businessPartnerJson
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void businessPartnerUpdated(String businessPartnerId, String version, String businessPartnerJson) {
        publishAuditEvent(BUSINESS_PARTNER_UPDATED, businessPartnerId, version, businessPartnerId, businessPartnerJson);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void trustOnboardingDocumentUploaded(
        String documentId,
        String version,
        String businessPartnerId,
        String s3Key
    ) {
        logAuditEvent(TRUST_ONBOARDING_DOCUMENT_UPLOADED, businessPartnerId);
        var auditCommand = withCommonFields(TRUST_ONBOARDING_DOCUMENT_UPLOADED, documentId, version, businessPartnerId)
            .addAuditObjectDataS3(NEW, "BUSINESS_PARTNER_DOCUMENT", s3Key)
            .build();
        sender.auditEvent(auditCommand);
    }

    private void logAuditEvent(AuditUseCase useCase, String businessPartnerId) {
        log.info("Sending audit event: useCase={}, businessPartnerId={}", useCase.getName(), businessPartnerId);
    }

    private void publishAuditEvent(
        AuditUseCase useCase,
        String objectId,
        String uploadCount,
        String businessPartnerId,
        String entityJson
    ) {
        publishAuditEvent(useCase, objectId, uploadCount, businessPartnerId, entityJson, null);
    }

    private void publishAuditEvent(
        AuditUseCase useCase,
        String objectId,
        String uploadCount,
        String businessPartnerId,
        String entityJson,
        String document
    ) {
        logAuditEvent(useCase, businessPartnerId);
        var builder = withCommonFields(useCase, objectId, uploadCount, businessPartnerId).addAuditObjectDataJSON(
            NEW,
            useCase.getMetaFieldName(),
            entityJson
        );
        if (document != null) {
            builder.addAuditObjectDataValue(NEW, useCase.getDocumentFieldName(), document);
        }
        sender.auditEvent(builder.build());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void trustOnboardingSubmitted(
        String submissionId,
        String version,
        String businessPartnerId,
        String submissionJson,
        List<String> doiS3Keys
    ) {
        logAuditEvent(TRUST_ONBOARDING_SUBMITTED, businessPartnerId);
        var builder = withCommonFields(
            TRUST_ONBOARDING_SUBMITTED,
            submissionId,
            version,
            businessPartnerId
        ).addAuditObjectDataJSON(NEW, TRUST_ONBOARDING_SUBMITTED.getMetaFieldName(), submissionJson);
        for (var s3Key : doiS3Keys) {
            builder.addAuditObjectDataS3(NEW, "TRUST_ONBOARDING_DOCUMENT", s3Key);
        }
        sender.auditEvent(builder.build());
    }

    private CreateAuditRecordCommandBuilder withCommonFields(
        AuditUseCase useCase,
        String objectId,
        String uploadCount,
        String businessPartnerId
    ) {
        // create the builder
        var timestamp = Instant.now();
        var serviceName = kafkaProperties.getServiceName();
        var systemName = kafkaProperties.getSystemName();
        var builder = CreateAuditRecordCommandBuilder.createCommandBuilder(serviceName, systemName, timestamp);
        // add trigger infos
        var auditor = getCurrentAuditor(SecurityContextHolder.getContext().getAuthentication());
        if (auditor.isSystem()) {
            builder.setTriggerSystem(DEPARTMENT_NAME, systemName, serviceName);
        } else {
            builder.setTriggerUser(auditor.auditUserId(), auditor.identityProvider());
        }
        // common properties
        return builder
            .idempotenceId(objectId + "-" + useCase.getName() + "-" + timestamp)
            .setEventType(useCase.getEventType())
            .setContext(useCase.getName(), getCurrentTraceId())
            .setAuditObject(
                useCase.getAuditObjectType(),
                objectId,
                /* since we do not have a JPA version on status list we use uploadCount */ uploadCount
            )
            .addEventData(USE_CASE_CATEGORY_ID.getKey(), useCase.getCategory())
            .addEventData(BUSINESS_PARTNER_ID.getKey(), businessPartnerId);
    }

    private String getCurrentTraceId() {
        var span = tracer.currentSpan();
        if (span == null) {
            log.error("No current span available, cannot get trace id for audit record");
            return null;
        }
        return span.context().traceId();
    }
}
