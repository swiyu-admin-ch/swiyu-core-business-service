package ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification;

import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class ProtectedVerificationSubmissionDomainService {

    private static final String PROTECTED_VERIFICATION_SUBMISSION_WITH_ID_S_NOT_FOUND =
        "ProtectedVerificationSubmission with id '%s' not found.";
    private final ProtectedVerificationSubmissionRepository protectedVerificationSubmissionRepository;

    @Transactional(readOnly = true)
    public ProtectedVerificationSubmission getProtectedVerificationSubmission(UUID protectedVerificationSubmissionId) {
        return protectedVerificationSubmissionRepository
            .findById(protectedVerificationSubmissionId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    String.format(
                        PROTECTED_VERIFICATION_SUBMISSION_WITH_ID_S_NOT_FOUND,
                        protectedVerificationSubmissionId
                    )
                )
            );
    }
}
