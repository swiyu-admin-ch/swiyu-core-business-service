package ch.admin.bj.swiyu.core.business.modules.trust.service.mapper;

import ch.admin.bj.swiyu.core.business.modules.trust.domain.event.TiTrustAddDidSubmissionSubmittedEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.event.TiTrustOnboardingSubmissionAcceptedEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.event.TiVcSchemaSubmissionAcceptedEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.event.TiVqpsSubmissionAcceptedEventBuilder;
import ch.admin.bj.swiyu.messagetype.ti.*;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionSubmittedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSubmissionAcceptedEvent;
import jakarta.annotation.Nonnull;
import java.util.UUID;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventMapper {

    public TiVcSchemaSubmissionAcceptedEvent mapToTiVcSchemaSubmissionAcceptedEvent(
        @NonNull UUID vcSchemaSubmissionId
    ) {
        return TiVcSchemaSubmissionAcceptedEventBuilder.create().vcSchemaSubmissionId(vcSchemaSubmissionId).build();
    }

    public TiTrustAddDidSubmissionSubmittedEvent mapToTiTrustAddDidSubmissionSubmittedEvent(
        @NonNull UUID trustAddDidSubmissionId
    ) {
        return TiTrustAddDidSubmissionSubmittedEventBuilder.create()
            .trustAddDidSubmissionId(trustAddDidSubmissionId)
            .build();
    }

    public TiTrustOnboardingSubmissionAcceptedEvent mapToTiTrustOnboardingSubmissionAcceptedEvent(
        @NonNull UUID trustOnboardingSubmissionId,
        @Nonnull UUID partnerId
    ) {
        return TiTrustOnboardingSubmissionAcceptedEventBuilder.create()
            .trustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .partnerId(partnerId)
            .build();
    }

    public TiVqpsSubmissionAcceptedEvent mapToTiVqpsSubmissionAcceptedEvent(@NonNull UUID vqpsSubmissionId) {
        return TiVqpsSubmissionAcceptedEventBuilder.create().vqpsSubmissionId(vqpsSubmissionId).build();
    }
}
