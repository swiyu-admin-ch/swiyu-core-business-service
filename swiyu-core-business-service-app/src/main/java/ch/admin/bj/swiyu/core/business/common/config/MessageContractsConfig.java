package ch.admin.bj.swiyu.core.business.common.config;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContracts;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bj.swiyu.messagetype.ti.*;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingInformationRequestedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingRejectedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSucceededEvent;
import org.springframework.context.annotation.Configuration;

@JeapMessageProducerContract(value = TiVcSchemaSubmissionAcceptedEvent.TypeRef.class, encryptionKeyId = "messagingKey")
@JeapMessageProducerContract(
    value = TiTrustOnboardingSubmissionAcceptedEvent.TypeRef.class,
    encryptionKeyId = "messagingKey"
)
@JeapMessageProducerContract(
    value = TiTrustAddDidSubmissionSubmittedEvent.TypeRef.class,
    encryptionKeyId = "messagingKey"
)
@JeapMessageProducerContract(value = TiVqpsSubmissionAcceptedEvent.TypeRef.class, encryptionKeyId = "messagingKey")
@JeapMessageConsumerContracts(
    {
        TiVcSchemaPublicationSucceededEvent.TypeRef.class,
        TiVcSchemaPublicationFailedEvent.TypeRef.class,
        TiTrustOnboardingInformationRequestedEvent.TypeRef.class,
        TiTrustOnboardingRejectedEvent.TypeRef.class,
        TiTrustOnboardingSucceededEvent.TypeRef.class,
        TiTrustAddDidSubmissionAcceptedEvent.TypeRef.class,
        TiTrustAddDidSubmissionRejectedEvent.TypeRef.class,
        TiVqpsPublicationSucceededEvent.TypeRef.class,
        TiVqpsPublicationFailedEvent.TypeRef.class,
    }
)
@Configuration
public class MessageContractsConfig {}
