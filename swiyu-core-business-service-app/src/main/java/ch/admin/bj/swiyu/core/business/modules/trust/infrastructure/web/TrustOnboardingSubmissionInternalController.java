package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "TrustOnboardingSubmission", description = "TrustOnboardingSubmission API")
@RequestMapping("/api/v1/internal/trust/trust-onboarding-submission")
public class TrustOnboardingSubmissionInternalController {

    private final TrustOnboardingService trustOnboardingService;
    private final AuthSupport authSupport;

    @PostMapping
    @PreAuthorize("hasRoleForPartner('trustonboardingsubmission','write', #submission.partnerId())")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(
        responseCode = "400",
        description = "Business validation failed. Possible error codes: DATA_INVALID.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorized. Possible error codes: RESOURCE_FORBIDDEN.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    public TrustOnboardingSubmissionDto createOnboardingSubmission(
        @Valid @RequestBody TrustOnboardingSubmissionRequestDto submission
    ) {
        return trustOnboardingService.createTrustOnboardingSubmission(submission);
    }

    @PreAuthorize("hasRole('trustonboardingsubmission','read')")
    @GetMapping
    @Operation(summary = "Get all TrustOnboardingSubmissions for partner.")
    @PageableAsQueryParam
    @SuppressWarnings("java:S1192") // ignore string literal duplication issue for auth checks
    public PagedModel<TrustOnboardingSubmissionListItemDto> getTrustOnboardings(
        @Valid @ParameterObject TrustOnboardingSubmissionFilterDto filter,
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        if (authSupport.hasRoleForAllPartners("trustonboardingsubmission", "read")) {
            return new PagedModel<>(trustOnboardingService.getAllTrustOnboardings(filter, pageable));
        } else {
            var authorizedPartnerIds = authSupport.getPartnerIdsForRole("trustonboardingsubmission", "read");
            if (filter.businessPartnerIds() == null) {
                filter = filter.of(authorizedPartnerIds);
            } else {
                // validate that authorization for all requested BP is available
                if (!(new HashSet<>(authorizedPartnerIds).containsAll(filter.businessPartnerIds()))) {
                    throw new AccessDeniedException(
                        "Permission has not been granted to access all of the requested business partners."
                    );
                }
            }
            return new PagedModel<>(trustOnboardingService.getAllTrustOnboardings(filter, pageable));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('trustonboardingsubmission','read')")
    @PostAuthorize("hasRoleForPartner('trustonboardingsubmission','read', returnObject.partnerId())")
    @Operation(summary = "Get specific TrustOnboardingSubmission by ID")
    public TrustOnboardingSubmissionDto getTrustOnboardingSubmission(@PathVariable @Valid UUID id) {
        return trustOnboardingService.getTrustOnboardingSubmission(id);
    }

    @PreAuthorize("hasRoleForPartner('trustonboardingsubmission','write', #submission.partnerId())")
    @PutMapping("/{id}")
    @Operation(summary = "Update")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(
        responseCode = "400",
        description = "Business validation failed. Possible error codes: DATA_INVALID.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorized. Possible error codes: RESOURCE_FORBIDDEN.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Submission not found. Possible error codes: RESOURCE_NOT_FOUND.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    public TrustOnboardingSubmissionDto updateTrustOnboardingSubmission(
        @PathVariable @Valid UUID id,
        @Valid @RequestBody TrustOnboardingSubmissionRequestDto submission
    ) {
        validateWritePermission(id);
        return trustOnboardingService.updateTrustOnboardingSubmission(id, submission);
    }

    @PreAuthorize("hasRole('trustonboardingsubmission','write')")
    @PostMapping("/{id}/submit")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(
        responseCode = "400",
        description = "Business validation failed. Possible error codes: DATA_INVALID.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorized. Possible error codes: RESOURCE_FORBIDDEN.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Submission not found. Possible error codes: RESOURCE_NOT_FOUND.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    @ApiResponse(
        responseCode = "409",
        description = "Concurrent update detected. Possible error codes: DATA_INVALID.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    public void submit(@PathVariable @Valid UUID id, @Valid @RequestBody TrustOnboardingSubmitRequestDto submission) {
        validateWritePermission(id);
        trustOnboardingService.submit(id, submission);
    }

    @SuppressWarnings("java:S1192") // ignore string literal duplication issue for auth checks
    private void validateWritePermission(UUID trustOnboardingSubmissionId) throws AccessDeniedException {
        var partnerId = trustOnboardingService.getTrustOnboardingSubmission(trustOnboardingSubmissionId).partnerId();
        authSupport.validateHasRoleForPartner("trustonboardingsubmission", "write", partnerId);
    }
}
