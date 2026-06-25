package ch.admin.bj.swiyu.core.business.modules.trust.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TrustOnboardingSubmissionAcceptedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TrustOnboardingSubmissionAcceptedReferences;
import java.util.UUID;

public class TiTrustOnboardingSubmissionAcceptedEventBuilder
    extends AvroDomainEventBuilder<
        TiTrustOnboardingSubmissionAcceptedEventBuilder,
        TiTrustOnboardingSubmissionAcceptedEvent
    >
{

    private UUID trustOnboardingSubmissionId;
    private UUID partnerId;
    private boolean isIdempotenceIdOverwritten;

    private TiTrustOnboardingSubmissionAcceptedEventBuilder() {
        super(TiTrustOnboardingSubmissionAcceptedEvent::new);
    }

    public static TiTrustOnboardingSubmissionAcceptedEventBuilder create() {
        return new TiTrustOnboardingSubmissionAcceptedEventBuilder();
    }

    public TiTrustOnboardingSubmissionAcceptedEventBuilder trustOnboardingSubmissionId(
        UUID trustOnboardingSubmissionId
    ) {
        this.trustOnboardingSubmissionId = trustOnboardingSubmissionId;
        return this;
    }

    public TiTrustOnboardingSubmissionAcceptedEventBuilder partnerId(UUID partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    @Override
    public TiTrustOnboardingSubmissionAcceptedEventBuilder idempotenceId(String idempotenceId) {
        isIdempotenceIdOverwritten = true;
        return super.idempotenceId(idempotenceId);
    }

    @Override
    protected String getServiceName() {
        return EventBuilderProperties.SERVICE_NAME;
    }

    @Override
    protected String getSystemName() {
        return EventBuilderProperties.SYSTEM_NAME;
    }

    @Override
    protected TiTrustOnboardingSubmissionAcceptedEventBuilder self() {
        return this;
    }

    @Override
    public TiTrustOnboardingSubmissionAcceptedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.trustOnboardingSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.trustOnboardingSubmissionId");
        }
        if (this.partnerId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.partnerId");
        }
        TrustOnboardingSubmissionAcceptedReferences declarationReferences =
            TrustOnboardingSubmissionAcceptedReferences.newBuilder().build();
        TrustOnboardingSubmissionAcceptedPayload declarationPayload =
            TrustOnboardingSubmissionAcceptedPayload.newBuilder()
                .setTrustOnboardingSubmissionId(trustOnboardingSubmissionId)
                .setPartnerId(partnerId)
                .build();
        setReferences(declarationReferences);
        setPayload(declarationPayload);
        return super.build();
    }
}
