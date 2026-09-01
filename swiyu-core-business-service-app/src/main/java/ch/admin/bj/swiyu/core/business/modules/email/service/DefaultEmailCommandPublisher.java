package ch.admin.bj.swiyu.core.business.modules.email.service;

import static ch.admin.bj.swiyu.core.business.modules.email.domain.EmailType.*;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.modules.email.config.MailProperties;
import ch.admin.bj.swiyu.core.business.modules.email.domain.EmailContentRenderer;
import ch.admin.bj.swiyu.core.business.modules.email.domain.EmailType;
import ch.admin.bj.swiyu.core.business.modules.email.domain.TiSendEmailCommandBuilder;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommand;
import ch.admin.bj.swiyu.messagetype.ti.common.BeanReferenceMessageKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composes and publishes the partner notification emails.
 *
 * <p>The recipient is resolved here rather than at the trigger points: every caller would otherwise
 * repeat the same lookup, and the rule "the recipient is the contact person of the business partner"
 * would be spread over eight classes instead of living in one.
 */
@Component
@AllArgsConstructor
@Slf4j
@JeapMessageProducerContract(
    value = TiSendEmailCommand.TypeRef.class,
    topic = TiSendEmailCommand.TypeRef.TOPIC_TI_SEND_EMAIL,
    encryptionKeyId = "messagingKey"
)
public class DefaultEmailCommandPublisher implements EmailCommandPublisher {

    private final TransactionalOutbox outbox;
    private final EmailContentRenderer renderer;
    private final MailProperties mailProperties;
    private final FunctionalityProperties functionalityProperties;
    private final BusinessPartnerService businessPartnerService;
    private final SentNotificationService sentNotificationService;

    /**
     * Number of days before expiry at which the first, differently worded reminder goes out. The later
     * ones repeat the same text with a shorter period, so they share one type.
     *
     * <p>The reviewed text of the initial reminder spells this period out ("ab heute 180 Tage") rather
     * than interpolating it, so the two must be changed together.
     */
    private static final int INITIAL_REMINDER_DAYS = 180;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void submissionAccepted(UUID partnerId) {
        publish(SUBMISSION_ACCEPTED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustRegistrationSucceeded(UUID partnerId) {
        publish(TRUST_REGISTRATION_SUCCEEDED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustRegistrationRejected(UUID partnerId) {
        publish(TRUST_REGISTRATION_REJECTED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustRegistrationInformationRequested(UUID partnerId) {
        publish(TRUST_REGISTRATION_INFORMATION_REQUESTED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustProfileChangeSucceeded(UUID partnerId) {
        publish(TRUST_PROFILE_CHANGE_SUCCEEDED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustProfileChangeRejected(UUID partnerId) {
        publish(TRUST_PROFILE_CHANGE_REJECTED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustProfileChangeInformationRequested(UUID partnerId) {
        publish(TRUST_PROFILE_CHANGE_INFORMATION_REQUESTED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustRenewalSucceeded(UUID partnerId) {
        publish(TRUST_RENEWAL_SUCCEEDED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustIdentityExpired(UUID partnerId) {
        publish(TRUST_IDENTITY_EXPIRED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustAdditionalDidAdded(UUID partnerId) {
        publish(TRUST_ADDITIONAL_DID_ADDED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustProtectedVerificationAhvApproved(UUID partnerId) {
        publish(TRUST_PROTECTED_VERIFICATION_AHV_APPROVED, partnerId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void trustProtectedVerificationAhvRejected(UUID partnerId) {
        publish(TRUST_PROTECTED_VERIFICATION_AHV_REJECTED, partnerId);
    }

    @Override
    @Transactional
    public void trustReviewDelayed(UUID partnerId, UUID trustOnboardingSubmissionId, int delayDurationDays) {
        // The reviewed text names no waiting period, so the number of days does not reach the template.
        // It stays in the idempotence id: raising the threshold later must reach partners who were
        // already told about the shorter delay.
        var idempotenceId = "%s-%s-%s-%d".formatted(
            TRUST_REVIEW_DELAYED,
            partnerId,
            trustOnboardingSubmissionId,
            delayDurationDays
        );
        publishScheduled(TRUST_REVIEW_DELAYED, partnerId, idempotenceId, Map.of());
    }

    @Override
    @Transactional
    public void trustRenewalReminder(UUID partnerId, Instant validUntil, int daysUntilExpiration) {
        var emailType =
            daysUntilExpiration == INITIAL_REMINDER_DAYS ? TRUST_RENEWAL_REMINDER_INITIAL : TRUST_RENEWAL_REMINDER;
        // validUntil is in the id so that a renewed identity starts a fresh series: without it the
        // partner would be reminded once in their lifetime and never again for the next period.
        var idempotenceId = "%s-%s-%s-%d".formatted(
            emailType,
            partnerId,
            validUntil.toEpochMilli(),
            daysUntilExpiration
        );
        // Only the repeated reminder counts the remaining days down; the initial one states its period
        // in the reviewed text and would leave the variable unused.
        Map<String, Object> variables =
            emailType == TRUST_RENEWAL_REMINDER_INITIAL
                ? Map.of()
                : Map.of("expirationDurationDays", daysUntilExpiration);
        publishScheduled(emailType, partnerId, idempotenceId, variables);
    }

    /**
     * Publishes a scheduled reminder under a composed, reproducible idempotence id.
     *
     * <p>Differs from {@link #publish} in two points only: the id is set instead of generated, and the
     * reminder is skipped if that id was already sent. Every guard of the event driven path - the
     * functionality flag, the partner id, the recipient - applies here unchanged.
     */
    private void publishScheduled(
        EmailType emailType,
        UUID partnerId,
        String idempotenceId,
        Map<String, Object> durationVariables
    ) {
        if (sentNotificationService.alreadySent(idempotenceId)) {
            log.debug("Email {} was already sent for partner '{}', skipping", emailType, partnerId);
            return;
        }
        publish(emailType, partnerId, idempotenceId, durationVariables);
    }

    private void publish(EmailType emailType, UUID partnerId) {
        publish(emailType, partnerId, null, Map.of());
    }

    private void publish(
        EmailType emailType,
        UUID partnerId,
        String idempotenceId,
        Map<String, Object> durationVariables
    ) {
        if (Boolean.FALSE.equals(functionalityProperties.emailEnabled())) {
            log.debug("Functionality 'email' is disabled, not sending email {}", emailType);
            return;
        }
        if (partnerId == null) {
            log.error("Cannot send email {} without a partner id", emailType);
            return;
        }
        var recipientEmail = businessPartnerService.getContactEmail(partnerId);
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.error("Business partner '{}' has no contact email, cannot send email {}", partnerId, emailType);
            return;
        }

        Map<String, Object> variables = new HashMap<>(durationVariables);
        variables.put("contactEmail", mailProperties.getFrom());
        variables.put("servicePortalPartnerUrl", mailProperties.getServicePortal().partnerUrl(partnerId));
        var email = renderer.render(emailType, variables, mailProperties.getSubjectPrefix());

        var builder = TiSendEmailCommandBuilder.create();
        if (idempotenceId != null) {
            builder.idempotenceId(idempotenceId);
        }
        var command = builder
            .partnerId(partnerId)
            .emailType(emailType.name())
            .to(List.of(recipientEmail))
            .from(mailProperties.getFrom())
            .replyTo(mailProperties.getReplyTo())
            .subject(email.subject())
            .sentAt(Instant.now())
            .plainTextMessage(email.plainTextMessage())
            .build();

        var topic = TiSendEmailCommand.TypeRef.DEFAULT_TOPIC;
        log.info("Publishing email {} for partner '{}' to topic {}", emailType, partnerId, topic);
        outbox.sendMessage(
            command,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiSendEmailCommand.TypeRef.SYSTEM_NAME)
                .setName(topic)
                .setId(partnerId.toString())
                .build(),
            topic
        );
    }
}
