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

    private void publish(EmailType emailType, UUID partnerId) {
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

        var variables = Map.<String, Object>of(
            "contactEmail",
            mailProperties.getFrom(),
            "servicePortalPartnerUrl",
            mailProperties.getServicePortal().partnerUrl(partnerId)
        );
        var email = renderer.render(emailType, variables, mailProperties.getSubjectPrefix());

        var command = TiSendEmailCommandBuilder.create()
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
