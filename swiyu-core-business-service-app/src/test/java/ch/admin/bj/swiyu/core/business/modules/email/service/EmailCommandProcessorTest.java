package ch.admin.bj.swiyu.core.business.modules.email.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.messaging.avro.AvroMessageIdentity;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommand;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommandPayload;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the order of the two steps and the failure path.
 *
 * <p>Not covered here: that a redelivered command is skipped. That is jEAP's
 * {@code @IdempotentMessageHandler} and is tested by jEAP itself - plain Mockito would not apply the
 * aspect anyway.
 */
class EmailCommandProcessorTest {

    private static final String IDEMPOTENCE_ID = "1e0b7a02-0000-0000-0000-000000000001";
    private static final UUID PARTNER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String RECIPIENT = "contact.person@partner.example.com";

    private AuditPublisher auditPublisher;
    private SentNotificationService sentNotificationService;
    private EmailSendService emailSendService;
    private EmailCommandProcessor processor;

    @BeforeEach
    void setUp() {
        auditPublisher = mock(AuditPublisher.class);
        sentNotificationService = mock(SentNotificationService.class);
        emailSendService = mock(EmailSendService.class);
        processor = processorWithEmailFunctionality(true);
    }

    private EmailCommandProcessor processorWithEmailFunctionality(boolean emailEnabled) {
        return new EmailCommandProcessor(
            sentNotificationService,
            emailSendService,
            new FunctionalityProperties(emailEnabled),
            auditPublisher
        );
    }

    @Test
    void sendsTheEmailAndRecordsIt() {
        processor.process(command());

        verify(emailSendService).send(any());
        verify(sentNotificationService).createEmailSentNotification(eq(IDEMPOTENCE_ID), any());
    }

    @Test
    void recordsOnlyAfterTheEmailWasSent() {
        processor.process(command());

        // Recording first would leave a record behind for an email that never left, should the send
        // fail without rolling back.
        var inOrder = inOrder(emailSendService, sentNotificationService);
        inOrder.verify(emailSendService).send(any());
        inOrder.verify(sentNotificationService).createEmailSentNotification(any(), any());
    }

    @Test
    void usesTheIdempotenceIdFromTheMessageIdentityNotFromThePayload() {
        processor.process(command());

        verify(sentNotificationService).createEmailSentNotification(eq(IDEMPOTENCE_ID), any());
    }

    @Test
    void propagatesTheExceptionWhenSendingFailsSoNothingIsRecorded() {
        doThrow(new IllegalStateException("smtp down")).when(emailSendService).send(any());
        var command = command();

        // The exception has to reach the caller - only then does Spring roll back both the idempotence
        // record and the SentNotification.
        assertThatThrownBy(() -> processor.process(command)).isInstanceOf(IllegalStateException.class);

        verify(sentNotificationService, never()).createEmailSentNotification(any(), any());
    }

    @Test
    void discardsTheCommandWhenTheFunctionalityIsDisabled() {
        var disabledProcessor = processorWithEmailFunctionality(false);

        disabledProcessor.process(command());

        // Commands published before the flag was switched off stay on the topic - without this check
        // they would still be sent after a restart.
        verifyNoInteractions(emailSendService, sentNotificationService);
    }

    private static TiSendEmailCommand command() {
        var identity = mock(AvroMessageIdentity.class);
        when(identity.getIdempotenceId()).thenReturn(IDEMPOTENCE_ID);

        var payload = new TiSendEmailCommandPayload(
            PARTNER_ID,
            "SUBMISSION_ACCEPTED",
            List.of(RECIPIENT),
            "registries@swiyu.admin.ch",
            "registries@swiyu.admin.ch",
            "Antrag eingereicht",
            Instant.now(),
            "Guten Tag"
        );

        var command = mock(TiSendEmailCommand.class);
        when(command.getIdentity()).thenReturn(identity);
        when(command.getPayload()).thenReturn(payload);
        return command;
    }
}
