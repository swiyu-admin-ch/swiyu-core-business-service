package ch.admin.bj.swiyu.core.business.common.audit;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.admin.bit.jeap.audit.record.create.*;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
class AuditPublisherIT {

    @Autowired
    AuditPublisher auditPublisher;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @MockitoBean // registers a callback so we can verify the sent message
    JeapKafkaMessageCallback kafkaMsgCallback;

    @Autowired
    Tracer tracer;

    @BeforeEach
    void setUp() {
        var messages = deferredMessageRepository.findAll();
        deferredMessageRepository.deleteAllById(
            messages.stream().map(DeferredMessage::getId).collect(Collectors.toSet())
        );
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void statusListEntryChanged() {
        // GIVEN
        var statusListEntryJson = "{\"id\":\"statuslist-1\"}";
        var statusListVc = "dummy-vc";

        // WHEN
        var traceId = executeWithTracing(() ->
            auditPublisher.statusListEntryChanged("sl-1", "1", "bp-1", statusListEntryJson, statusListVc)
        );

        // THEN
        // check outbox
        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        // get send event
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        // check message and its payload
        assertThat(msg.getPublisher().getSystem()).isEqualTo("ti");
        assertThat(msg.getPublisher().getService()).isEqualTo("swiyu-core-business-service");
        assertThat(msg.getPayload().getEvent().getType()).isEqualTo(AuditEventType.MODIFIED);
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(STATUS_LIST_CHANGED.getName());
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(STATUS_LIST_CHANGED.getCategory());
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo("bp-1");
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getId()).isEqualTo("subject:familyName,givenName");
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getIdentityProvider()).isEqualTo("test-issuer");
        assertThat(msg.getPayload().getAuditedData().getId()).isEqualTo("sl-1");
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(STATUS_LIST_CHANGED.getAuditObjectType());
        assertThat(getAuditObjectDataJSON(msg, "STATUS_LIST_META").getRole()).isEqualTo(AuditObjectDataRole.NEW);
        assertThat(getAuditObjectDataJSON(msg, "STATUS_LIST_META").getJsonAsUTF8().toString()).isNotBlank();
        assertThat(getAuditObjectDataValue(msg, "STATUS_LIST_JWT").getValue()).isEqualTo(statusListVc);
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void statusListEntryCreated() {
        // GIVEN
        var statusListEntryId = UUID.randomUUID().toString();
        var businessPartnerId = UUID.randomUUID().toString();
        var statusListEntryJson = "{\"id\":\"statuslist-1\"}";

        // WHEN
        var traceId = executeWithTracing(() ->
            auditPublisher.statusListEntryCreated(statusListEntryId, businessPartnerId, statusListEntryJson)
        );

        // THEN
        // check outbox
        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        // get send event
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        // check message and its payload
        assertThat(msg.getPublisher().getSystem()).isEqualTo("ti");
        assertThat(msg.getPublisher().getService()).isEqualTo("swiyu-core-business-service");
        assertThat(msg.getPayload().getEvent().getType()).isEqualTo(AuditEventType.CREATED);
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(STATUS_LIST_CREATED.getName());
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(STATUS_LIST_CREATED.getCategory());
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo(businessPartnerId);
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getId()).isEqualTo("subject:familyName,givenName");
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getIdentityProvider()).isEqualTo("test-issuer");
        assertThat(msg.getPayload().getAuditedData().getId()).isEqualTo(statusListEntryId);
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(STATUS_LIST_CREATED.getAuditObjectType());
        assertThat(getAuditObjectDataJSON(msg, "STATUS_LIST_META").getRole()).isEqualTo(AuditObjectDataRole.NEW);
        assertThat(getAuditObjectDataJSON(msg, "STATUS_LIST_META").getJsonAsUTF8().toString()).isNotBlank();
        assertThat(msg.getPayload().getAuditedData().getObjectData()).noneMatch(
            d -> d instanceof AuditObjectDataValue v && "STATUS_LIST_JWT".equals(v.getName())
        );
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void identifierEntryCreated() {
        // GIVEN
        var identifierEntryId = UUID.randomUUID().toString();
        var businessPartnerId = UUID.randomUUID().toString();
        var identifierEntryJson = "{\"id\":\"identifier-1\"}";

        // WHEN
        var traceId = executeWithTracing(() ->
            auditPublisher.identifierEntryCreated(identifierEntryId, businessPartnerId, identifierEntryJson)
        );

        // THEN
        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getPublisher().getSystem()).isEqualTo("ti");
        assertThat(msg.getPublisher().getService()).isEqualTo("swiyu-core-business-service");
        assertThat(msg.getPayload().getEvent().getType()).isEqualTo(AuditEventType.CREATED);
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(IDENTIFIER_ENTRY_CREATED.getName());
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(
            IDENTIFIER_ENTRY_CREATED.getCategory()
        );
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo(businessPartnerId);
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getId()).isEqualTo("subject:familyName,givenName");
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getIdentityProvider()).isEqualTo("test-issuer");
        assertThat(msg.getPayload().getAuditedData().getId()).isEqualTo(identifierEntryId);
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(
            IDENTIFIER_ENTRY_CREATED.getAuditObjectType()
        );
        assertThat(getAuditObjectDataJSON(msg, "IDENTIFIER_ENTRY_META").getRole()).isEqualTo(AuditObjectDataRole.NEW);
        assertThat(getAuditObjectDataJSON(msg, "IDENTIFIER_ENTRY_META").getJsonAsUTF8().toString()).isNotBlank();
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void identifierEntryChanged() {
        // GIVEN
        var identifierEntryId = UUID.randomUUID().toString();
        var businessPartnerId = UUID.randomUUID().toString();
        var identifierEntryJson = "{\"id\":\"identifier-1\"}";
        var didLog = "[{\"versionId\":\"1\"}]";

        // WHEN
        var traceId = executeWithTracing(() ->
            auditPublisher.identifierEntryChanged(
                identifierEntryId,
                "1",
                businessPartnerId,
                identifierEntryJson,
                didLog
            )
        );

        // THEN
        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getPublisher().getSystem()).isEqualTo("ti");
        assertThat(msg.getPublisher().getService()).isEqualTo("swiyu-core-business-service");
        assertThat(msg.getPayload().getEvent().getType()).isEqualTo(AuditEventType.MODIFIED);
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(IDENTIFIER_ENTRY_CHANGED.getName());
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(
            IDENTIFIER_ENTRY_CHANGED.getCategory()
        );
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo(businessPartnerId);
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getId()).isEqualTo("subject:familyName,givenName");
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getIdentityProvider()).isEqualTo("test-issuer");
        assertThat(msg.getPayload().getAuditedData().getId()).isEqualTo(identifierEntryId);
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(
            IDENTIFIER_ENTRY_CHANGED.getAuditObjectType()
        );
        assertThat(getAuditObjectDataJSON(msg, "IDENTIFIER_ENTRY_META").getRole()).isEqualTo(AuditObjectDataRole.NEW);
        assertThat(getAuditObjectDataJSON(msg, "IDENTIFIER_ENTRY_META").getJsonAsUTF8().toString()).isNotBlank();
        assertThat(getAuditObjectDataValue(msg, "IDENTIFIER_ENTRY_DID_DOC").getValue()).isEqualTo(didLog);
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void identifierEntryDescriptionChanged() {
        // GIVEN
        var identifierEntryId = UUID.randomUUID().toString();
        var businessPartnerId = UUID.randomUUID().toString();
        var identifierEntryJson = "{\"id\":\"identifier-1\"}";

        // WHEN
        var traceId = executeWithTracing(() ->
            auditPublisher.identifierEntryDescriptionChanged(
                identifierEntryId,
                "0",
                businessPartnerId,
                identifierEntryJson
            )
        );

        // THEN
        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getPublisher().getSystem()).isEqualTo("ti");
        assertThat(msg.getPublisher().getService()).isEqualTo("swiyu-core-business-service");
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(
            IDENTIFIER_ENTRY_DESCRIPTION_CHANGED.getName()
        );
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(
            IDENTIFIER_ENTRY_DESCRIPTION_CHANGED.getCategory()
        );
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo(businessPartnerId);
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getId()).isEqualTo("subject:familyName,givenName");
        assertThat(((AuditUser) msg.getPayload().getTrigger()).getIdentityProvider()).isEqualTo("test-issuer");
        assertThat(msg.getPayload().getAuditedData().getId()).isEqualTo(identifierEntryId);
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(
            IDENTIFIER_ENTRY_DESCRIPTION_CHANGED.getAuditObjectType()
        );
        assertThat(getAuditObjectDataJSON(msg, "IDENTIFIER_ENTRY_META").getRole()).isEqualTo(AuditObjectDataRole.NEW);
        assertThat(getAuditObjectDataJSON(msg, "IDENTIFIER_ENTRY_META").getJsonAsUTF8().toString()).isNotBlank();
    }

    private String executeWithTracing(Runnable r) {
        var span = tracer.nextSpan().start();
        try (var _ = tracer.withSpan(span)) {
            r.run();
        }
        return span.context().traceId();
    }

    private AuditObjectDataJSON getAuditObjectDataJSON(CreateAuditRecordCommand message, String name) {
        return message
            .getPayload()
            .getAuditedData()
            .getObjectData()
            .stream()
            .filter(AuditObjectDataJSON.class::isInstance)
            .map(o -> (AuditObjectDataJSON) o)
            .filter(o -> o.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void businessPartnerRegistered() {
        var businessPartnerJson = "{\"id\":\"bp-1\"}";

        var traceId = executeWithTracing(() ->
            auditPublisher.businessPartnerRegistered("bp-1", "0", businessPartnerJson)
        );

        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getPayload().getEvent().getType()).isEqualTo(AuditEventType.CREATED);
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(
            BUSINESS_PARTNER_REGISTERED.getName()
        );
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(
            BUSINESS_PARTNER_REGISTERED.getCategory()
        );
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo("bp-1");
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(
            BUSINESS_PARTNER_REGISTERED.getAuditObjectType()
        );
        assertThat(getAuditObjectDataJSON(msg, "BUSINESS_PARTNER_DATA").getJsonAsUTF8().toString()).isNotBlank();
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void businessPartnerUpdated() {
        var businessPartnerJson = "{\"id\":\"bp-2\"}";

        var traceId = executeWithTracing(() -> auditPublisher.businessPartnerUpdated("bp-2", "1", businessPartnerJson));

        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getPayload().getEvent().getType()).isEqualTo(AuditEventType.MODIFIED);
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(BUSINESS_PARTNER_UPDATED.getName());
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(
            BUSINESS_PARTNER_UPDATED.getCategory()
        );
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo("bp-2");
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(
            BUSINESS_PARTNER_UPDATED.getAuditObjectType()
        );
        assertThat(getAuditObjectDataJSON(msg, "BUSINESS_PARTNER_DATA").getJsonAsUTF8().toString()).isNotBlank();
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void trustOnboardingDocumentUploaded() {
        var s3Key = "partner-1/trust/submission-1/doc-1-file.pdf";

        var traceId = executeWithTracing(() ->
            auditPublisher.trustOnboardingDocumentUploaded("doc-1", "0", "bp-1", s3Key)
        );

        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getPayload().getEvent().getType()).isEqualTo(AuditEventType.MODIFIED);
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(
            TRUST_ONBOARDING_DOCUMENT_UPLOADED.getName()
        );
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(
            TRUST_ONBOARDING_DOCUMENT_UPLOADED.getCategory()
        );
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo("bp-1");
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(
            TRUST_ONBOARDING_DOCUMENT_UPLOADED.getAuditObjectType()
        );
        assertThat(getAuditObjectDataS3(msg, "BUSINESS_PARTNER_DOCUMENT").getObjectReference()).isEqualTo(s3Key);
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken
    void trustOnboardingSubmitted() {
        var submissionJson = "{\"id\":\"sub-1\"}";
        var s3Key1 = "partner-1/trust/sub-1/doc-1-doi.pdf";
        var s3Key2 = "partner-1/trust/sub-1/doc-2-doi.pdf";

        var traceId = executeWithTracing(() ->
            auditPublisher.trustOnboardingSubmitted(
                "sub-1",
                "2",
                "bp-1",
                submissionJson,
                java.util.List.of(s3Key1, s3Key2)
            )
        );

        var messages = deferredMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        var messageCaptor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getPayload().getEvent().getType()).isEqualTo(AuditEventType.MODIFIED);
        assertThat(msg.getPayload().getEvent().getContext().getUseCase()).isEqualTo(
            TRUST_ONBOARDING_SUBMITTED.getName()
        );
        assertThat(msg.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
        assertThat(getEventDataByKey(msg, USE_CASE_CATEGORY_ID.getKey())).isEqualTo(
            TRUST_ONBOARDING_SUBMITTED.getCategory()
        );
        assertThat(getEventDataByKey(msg, BUSINESS_PARTNER_ID.getKey())).isEqualTo("bp-1");
        assertThat(msg.getPayload().getAuditedData().getType()).isEqualTo(
            TRUST_ONBOARDING_SUBMITTED.getAuditObjectType()
        );
        assertThat(
            getAuditObjectDataJSON(msg, "TRUST_ONBOARDING_SUBMISSION_DATA").getJsonAsUTF8().toString()
        ).isNotBlank();
        var s3Refs = getAuditObjectDataS3List(msg, "TRUST_ONBOARDING_DOCUMENT");
        assertThat(s3Refs).hasSize(2);
        assertThat(s3Refs.stream().map(AuditObjectDataS3::getObjectReference).toList()).containsExactlyInAnyOrder(
            s3Key1,
            s3Key2
        );
    }

    private AuditObjectDataValue getAuditObjectDataValue(CreateAuditRecordCommand message, String name) {
        return message
            .getPayload()
            .getAuditedData()
            .getObjectData()
            .stream()
            .filter(AuditObjectDataValue.class::isInstance)
            .map(o -> (AuditObjectDataValue) o)
            .filter(o -> o.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    private AuditObjectDataS3 getAuditObjectDataS3(CreateAuditRecordCommand message, String name) {
        return message
            .getPayload()
            .getAuditedData()
            .getObjectData()
            .stream()
            .filter(AuditObjectDataS3.class::isInstance)
            .map(o -> (AuditObjectDataS3) o)
            .filter(o -> o.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    private java.util.List<AuditObjectDataS3> getAuditObjectDataS3List(CreateAuditRecordCommand message, String name) {
        return message
            .getPayload()
            .getAuditedData()
            .getObjectData()
            .stream()
            .filter(AuditObjectDataS3.class::isInstance)
            .map(o -> (AuditObjectDataS3) o)
            .filter(o -> o.getName().equals(name))
            .toList();
    }

    private static String getEventDataByKey(CreateAuditRecordCommand message, String key) {
        return message
            .getPayload()
            .getEvent()
            .getEventData()
            .stream()
            .filter(d -> d.getKey().equals(key))
            .findFirst()
            .orElseThrow()
            .getValue();
    }
}
