package ch.admin.bj.swiyu.core.business.modules.trust.service.mapper;

import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType.PROFILE_CHANGE_MANDATORY;
import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType.PROFILE_CHANGE_VOLUNTARY;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustOnboardingMapper.toTrustOnboardingSubmissionDto;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.Signatory;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrustOnboardingMapperTest {

    @Test
    void toDtoMapsAllSubmissionTypes() {
        // Given
        var partnerId = UUID.randomUUID();
        var submissionId = UUID.randomUUID();
        var address = new Address("Musterstrasse", "Bern", "3000", "CH", null);
        var contact = new Contact("John", "Doe", "john@test.com", "+41 12 345 67 89", Language.DE);
        var signatory = new Signatory("John", "Doe", "+41 12 345 67 89", "john@test.com");
        var proofOfPossession = new ProofOfPossession("did:example:123", "nonce123");

        // Test REGISTRATION
        var registrationSubmission = new TrustOnboardingSubmission(
            submissionId,
            partnerId,
            Collections.singletonMap("default", "Test Entity"),
            address,
            "test@test.com",
            contact,
            null,
            false,
            List.of(proofOfPossession),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(signatory)
        );
        var registrationDto = toTrustOnboardingSubmissionDto(registrationSubmission);
        assertEquals(TrustOnboardingSubmissionTypeDto.REGISTRATION, registrationDto.type());

        // Test PROFILE_CHANGE_MANDATORY
        var mandatorySubmission = new TrustOnboardingSubmission(
            PROFILE_CHANGE_MANDATORY,
            submissionId,
            partnerId,
            Collections.singletonMap("default", "Test Entity"),
            address,
            "test@test.com",
            contact,
            null,
            false,
            List.of(proofOfPossession),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(signatory)
        );
        var mandatoryDto = toTrustOnboardingSubmissionDto(mandatorySubmission);
        assertEquals(TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_MANDATORY, mandatoryDto.type());

        // Test PROFILE_CHANGE_VOLUNTARY
        var voluntarySubmission = new TrustOnboardingSubmission(
            PROFILE_CHANGE_VOLUNTARY,
            submissionId,
            partnerId,
            Collections.singletonMap("default", "Test Entity"),
            address,
            "test@test.com",
            contact,
            null,
            false,
            List.of(proofOfPossession),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(signatory)
        );
        var voluntaryDto = toTrustOnboardingSubmissionDto(voluntarySubmission);
        assertEquals(TrustOnboardingSubmissionTypeDto.PROFILE_CHANGE_VOLUNTARY, voluntaryDto.type());

        // Test RENEWAL
        var renewalSubmission = new TrustOnboardingSubmission(
            TrustOnboardingSubmissionType.RENEWAL,
            submissionId,
            partnerId,
            Collections.singletonMap("default", "Test Entity"),
            address,
            "test@test.com",
            contact,
            null,
            false,
            List.of(proofOfPossession),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(signatory)
        );
        var renewalDto = toTrustOnboardingSubmissionDto(renewalSubmission);
        assertEquals(TrustOnboardingSubmissionTypeDto.RENEWAL, renewalDto.type());
    }
}
