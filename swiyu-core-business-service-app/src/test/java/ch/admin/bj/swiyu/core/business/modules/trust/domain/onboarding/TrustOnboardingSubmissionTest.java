package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType.PROFILE_CHANGE_VOLUNTARY;
import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType.REGISTRATION;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrustOnboardingSubmissionTest {

    @Test
    void newSubmission_hasDefaultTypeRegistration() {
        var submission = buildSubmission(REGISTRATION);
        assertThat(submission.getType()).isEqualTo(REGISTRATION);
    }

    @Test
    void updateType_changesTypeToProfileChangeVoluntary() {
        var submission = buildSubmission(PROFILE_CHANGE_VOLUNTARY);
        assertThat(submission.getType()).isEqualTo(PROFILE_CHANGE_VOLUNTARY);
    }

    private static TrustOnboardingSubmission buildSubmission(TrustOnboardingSubmissionType type) {
        return new TrustOnboardingSubmission(
            type,
            UUID.randomUUID(),
            UUID.randomUUID(),
            Map.of("default", "Entity"),
            new Address(),
            "valid@example.com",
            new Contact("John", "Doe", "john@example.com", "+41 79 123 45 67", Language.DE),
            null,
            false,
            List.of(new ProofOfPossession("did:example:123", UUID.randomUUID().toString())),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.com"))
        );
    }
}
