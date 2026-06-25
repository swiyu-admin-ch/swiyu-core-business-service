package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionInternalDto;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustAdditionalDidsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "TrustAddDidsSubmission Internal", description = "TrustAddDidsSubmission Internal API")
@RequestMapping("/api/v1/internal/submissions/trust-add-dids-submissions")
public class TrustAddDidsSubmissionInternalController {

    private final TrustAdditionalDidsService trustAdditionalDidsService;

    @GetMapping("/{trustAddDidsSubmissionId}")
    @PreAuthorize("hasRole('trustonboardingsubmission', 'read')")
    @Operation(summary = "Get submission with proof of possession status")
    public TrustAdditionalDidsSubmissionInternalDto getSubmission(@PathVariable UUID trustAddDidsSubmissionId) {
        return trustAdditionalDidsService.getSubmissionInternal(trustAddDidsSubmissionId);
    }
}
