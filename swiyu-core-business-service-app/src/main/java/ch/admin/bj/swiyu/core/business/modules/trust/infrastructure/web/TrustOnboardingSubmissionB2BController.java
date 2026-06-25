package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProofOfPossessionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProofOfPossessionSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "TrustOnboardingSubmission B2B", description = "TrustOnboardingSubmission B2B API")
@ConditionalOnProperty(name = "features.EIDARTFE_1220_PROOF_OF_POSSESSION", havingValue = "true")
@RequestMapping("/api/v1/trust/trust-onboarding-submission")
public class TrustOnboardingSubmissionB2BController {

    private final TrustOnboardingService trustOnboardingService;
    private final FeaturesProperties featuresProperties;
    private final AuthSupport authSupport;

    @PostMapping("/proof-of-possessions")
    @PreAuthorize("hasRole('trustonboardingsubmission', 'write')")
    @Operation(
        summary = "Upload ALL proof of possessions for a pending trust onboarding submission of the partner. " +
            "The proofs are generated with the didtoolbox"
    )
    public TrustOnboardingSubmissionDto submitProofOfPossessions(
        @RequestBody ProofOfPossessionSubmissionDto submissionDto
    ) {
        if (Boolean.FALSE.equals(featuresProperties.getEidartfe1220ProofOfPossession())) {
            throw new UnsupportedOperationException("Functionality for proof of possession is disabled");
        }
        var partnerId = authSupport.getPartnerIdForRole("trustonboardingsubmission", "write");
        return trustOnboardingService.submitProofOfPossessions(partnerId, submissionDto.proofOfPossessions());
    }

    @GetMapping("/proof-of-possessions")
    @PreAuthorize("hasRole('trustonboardingsubmission', 'read')")
    @Operation(
        summary = "Get all proof of possessions for the current " +
            "pending trust onboarding submission of the partner."
    )
    public List<ProofOfPossessionDto> getProofOfPossessions() {
        if (Boolean.FALSE.equals(featuresProperties.getEidartfe1220ProofOfPossession())) {
            throw new UnsupportedOperationException("Functionality for proof of possession is disabled");
        }
        UUID partnerId = authSupport.getPartnerIdForRole("trustonboardingsubmission", "read");
        return trustOnboardingService.getSubmissionByPartnerId(partnerId).proofOfPossessions();
    }
}
