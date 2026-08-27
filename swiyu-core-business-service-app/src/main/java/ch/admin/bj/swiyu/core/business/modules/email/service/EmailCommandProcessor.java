package ch.admin.bj.swiyu.core.business.modules.email.service;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import ch.admin.bj.swiyu.core.business.modules.email.domain.Email;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a consumed {@code TiSendEmailCommand} into a sent email.
 *
 * <p>{@code @IdempotentMessageHandler} keeps a redelivered command from sending a second email: the
 * aspect records the idempotence id before this method runs and skips the call if it is already
 * there. The record is written in this transaction, so a failing send rolls it back and the
 * redelivered command is retried cleanly - same as for the six event processors in
 * {@code modules/trust}.
 *
 * <p>SMTP is not a transaction participant, so one gap remains: a send that succeeds while the commit
 * fails leaves the email out and the record gone, and the redelivery sends it a second time. That is
 * the deliberate trade - a duplicate notification is an annoyance, a rejection that never arrives is
 * a business problem.
 *
 * <p>No audit record is written for a sent email yet. The audit format is not specified by
 * architecture; a dedicated story will define it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCommandProcessor {

    private final SentNotificationService sentNotificationService;
    private final EmailSendService emailSendService;
    private final FunctionalityProperties functionalityProperties;

    @Transactional
    @IdempotentMessageHandler
    public void process(TiSendEmailCommand command) {
        if (Boolean.FALSE.equals(functionalityProperties.emailEnabled())) {
            log.info("Functionality 'email' is disabled, discarding email {}", command.getPayload().getEmailType());
            return;
        }

        var email = Email.from(command.getPayload());

        emailSendService.send(email);

        sentNotificationService.createEmailSentNotification(command.getIdentity().getIdempotenceId(), email);
    }
}
