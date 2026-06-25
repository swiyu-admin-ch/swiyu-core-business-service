package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class TrustOnboardingSubmissionDomainService {

    private static final String TRUST_ONBOARDING_SUBMISSION_WITH_ID_S_NOT_FOUND =
        "TrustOnboardingSubmission with id '%s' not found.";
    private final TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;

    @Transactional(readOnly = true)
    public TrustOnboardingSubmission getTrustOnboardingSubmission(UUID trustOnboardingSubmissionId) {
        return trustOnboardingSubmissionRepository
            .findById(trustOnboardingSubmissionId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    String.format(TRUST_ONBOARDING_SUBMISSION_WITH_ID_S_NOT_FOUND, trustOnboardingSubmissionId)
                )
            );
    }

    @Transactional(readOnly = true)
    public TrustOnboardingSubmission getUnsubmittedTrustOnboardingSubmissionByPartner(UUID partnerId) {
        var trustOnboardingSubmission = trustOnboardingSubmissionRepository.findByPartnerIdAndStatusIn(
            partnerId,
            List.of(TrustOnboardingSubmissionStatus.UNSUBMITTED)
        );

        if (trustOnboardingSubmission == null) {
            throw new ResourceNotFoundException(
                "No UNSUBMITTED TrustOnboardingSubmission found for partner and therefore no submission of proof of possessions is possible"
            );
        }
        return trustOnboardingSubmission;
    }
}
