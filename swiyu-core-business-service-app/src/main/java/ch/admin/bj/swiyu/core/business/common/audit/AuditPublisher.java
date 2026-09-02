package ch.admin.bj.swiyu.core.business.common.audit;

import static ch.admin.bit.jeap.audit.record.create.AuditObjectDataRole.NEW;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditEventDataKey.BUSINESS_PARTNER_ID;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditEventDataKey.USE_CASE_CATEGORY_ID;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditUseCase.*;
import static ch.admin.bj.swiyu.core.business.common.audit.AuditorProvider.getCurrentAuditor;

import ch.admin.bit.jeap.audit.command.builder.CreateAuditRecordCommandBuilder;
import ch.admin.bit.jeap.audit.record.create.CreateAuditRecordCommand;
import ch.admin.bit.jeap.audit.transactional.outbox.CreateAuditRecordCommandTransactionOutboxSender;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
        String s3Key,
        String partnerDocumentJson
    ) {
        logAuditEvent(TRUST_ONBOARDING_DOCUMENT_UPLOADED, businessPartnerId);
        var auditCommand = withCommonFields(TRUST_ONBOARDING_DOCUMENT_UPLOADED, documentId, version, businessPartnerId)
            .addAuditObjectDataJSON(NEW, DataJsonFieldName.TRUST_ONBOARDING_DOCUMENT_META, partnerDocumentJson)
            .addAuditObjectDataS3(NEW, DataS3FieldName.TRUST_ONBOARDING_DOCUMENT, s3Key)
            .build();
        sender.auditEvent(auditCommand);
    }

    private void logAuditEvent(AuditUseCase useCase, String businessPartnerId) {
        log.info("Sending audit event: useCase={}, businessPartnerId={}", useCase.name(), businessPartnerId);
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
        ).addAuditObjectDataJSON(NEW, TRUST_ONBOARDING_SUBMITTED.getDataJsonFieldName(), submissionJson);
        for (var s3Key : doiS3Keys) {
            builder.addAuditObjectDataS3(NEW, DataS3FieldName.TRUST_ONBOARDING_DOCUMENT, s3Key);
        }
        sender.auditEvent(builder.build());
    }

    @Transactional
    public void emailSent(UUID businessPartnerId, String emailIdempotenceId, String emailJson) {
        logAuditEvent(EMAIL_SENT, businessPartnerId.toString());
        var builder = withCommonFields(
            EMAIL_SENT,
            emailIdempotenceId,
            "0", // emails do not have versions
            businessPartnerId.toString()
        ).addAuditObjectDataJSON(NEW, EMAIL_SENT.getDataJsonFieldName(), emailJson);
        sender.auditEvent(builder.build());
    }

    private void publishAuditEvent(
        AuditUseCase useCase,
        String objectId,
        String uploadCount,
        String businessPartnerId,
        String dataJson,
        String dataValue
    ) {
        logAuditEvent(useCase, businessPartnerId);
        var builder = withCommonFields(useCase, objectId, uploadCount, businessPartnerId).addAuditObjectDataJSON(
            NEW,
            useCase.getDataJsonFieldName(),
            dataJson
        );
        if (dataValue != null) {
            builder.addAuditObjectDataValue(NEW, useCase.getDataValueFieldName(), dataValue);
        }
        sender.auditEvent(builder.build());
    }

    private CreateAuditRecordCommandBuilder withCommonFields(
        AuditUseCase useCase,
        String objectId,
        String version,
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
            .idempotenceId(objectId + "-" + useCase.name() + "-" + timestamp)
            .setEventType(useCase.getEventType())
            .setContext(useCase.name(), getCurrentTraceId())
            .setAuditObject(useCase.getAuditObjectType(), objectId, version)
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
