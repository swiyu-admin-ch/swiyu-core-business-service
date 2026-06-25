package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionCreateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionResponseDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionUpdateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustAdditionalDidsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "TrustAddDidsSubmission B2B", description = "TrustAddDidsSubmission B2B API")
@ConditionalOnProperty(name = "features.EIDARTFE_1204_TRUST_ADD_DIDS", havingValue = "true")
@RequestMapping("/api/v1/trust/trust-add-dids-submissions")
public class TrustAddDidsSubmissionB2BController {

    private static final String ROLE_NAME = "trustonboardingsubmission";

    private final TrustAdditionalDidsService trustAdditionalDidsService;
    private final AuthSupport authSupport;

    @PostMapping
    @PreAuthorize("hasRole('trustonboardingsubmission', 'write')")
    @Operation(summary = "Create a new submission to add additional DIDs to a trusted partner")
    public TrustAdditionalDidsSubmissionResponseDto createSubmission(
        @Valid @RequestBody TrustAdditionalDidsSubmissionCreateRequestDto dto
    ) {
        var partnerId = authSupport.getPartnerIdForRole(ROLE_NAME, "write");
        return trustAdditionalDidsService.createSubmission(partnerId, dto);
    }

    @GetMapping("/{trustAddDidsSubmissionId}")
    @PreAuthorize("hasRole('trustonboardingsubmission', 'read')")
    @Operation(summary = "Get a trust add-DIDs submission by ID")
    public TrustAdditionalDidsSubmissionResponseDto getSubmission(@PathVariable UUID trustAddDidsSubmissionId) {
        var partnerId = authSupport.getPartnerIdForRole(ROLE_NAME, "read");
        return trustAdditionalDidsService.getSubmission(trustAddDidsSubmissionId, partnerId);
    }

    @PostMapping("/{trustAddDidsSubmissionId}")
    @PreAuthorize("hasRole('trustonboardingsubmission', 'write')")
    @Operation(summary = "Submit proofs of possession for the submission")
    public TrustAdditionalDidsSubmissionResponseDto submitWithProofsOfPossession(
        @PathVariable UUID trustAddDidsSubmissionId,
        @Valid @RequestBody TrustAdditionalDidsSubmissionUpdateRequestDto dto
    ) {
        var partnerId = authSupport.getPartnerIdForRole(ROLE_NAME, "write");
        return trustAdditionalDidsService.submitWithProofsOfPossession(trustAddDidsSubmissionId, partnerId, dto);
    }
}
