package ch.admin.bj.swiyu.core.business.modules.email.service;

import ch.admin.bj.swiyu.core.business.modules.email.domain.Email;
import ch.admin.bj.swiyu.core.business.modules.email.domain.SentNotification;
import ch.admin.bj.swiyu.core.business.modules.email.domain.SentNotificationRepository;
import ch.admin.bj.swiyu.core.business.modules.email.domain.SentNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Records which emails were sent.
 *
 * <p>This is a business record, not the duplicate guard - that is
 * {@code @IdempotentMessageHandler} on the processor. It answers which email a partner received and
 * when, and the scheduled reminders (EID-6628) decide from it whether a reminder already went out.
 * Unlike the framework's {@code idempotent_processing} table, which is purged after a retention
 * period, these rows are kept.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SentNotificationService {

    private final SentNotificationRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Whether the notification with that idempotence id already went out.
     *
     * <p>Used by the scheduled reminders to skip a partner who was already reminded. This is an
     * optimisation, not the guarantee: the guarantee is {@code @IdempotentMessageHandler} on the
     * processor plus the unique constraint on the column. Without this check the job would publish a
     * command every night for every partner in the window and rely on the consumer to drop them all.
     */
    @Transactional(readOnly = true)
    public boolean alreadySent(String idempotenceId) {
        return repository.existsByIdempotenceId(idempotenceId);
    }

    /**
     * Records a sent email.
     *
     * <p>Must run inside the caller's transaction, so the record shares the fate of the send: no
     * record for an email that never left, and no email without a record.
     *
     * <p>When it went out is {@code auditMetadata.createdAt} - the row is written in the same
     * transaction as the send, so a separate timestamp would hold the same instant.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createEmailSentNotification(String idempotenceId, Email email) {
        var notification = new SentNotification(
            idempotenceId,
            SentNotificationType.EMAIL,
            email.partnerId(),
            objectMapper.valueToTree(email)
        );
        repository.save(notification);
        log.debug("Recorded sent email {} with idempotence id '{}'", email.emailType(), idempotenceId);
    }
}
