package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.service.protectedverification.ProtectedVerificationSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.List;
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
@Tag(name = "ProtectedVerificationSubmission", description = "ProtectedVerificationSubmission API")
@RequestMapping("/api/v1/internal/trust/protected-verification-submissions")
public class ProtectedVerificationSubmissionInternalController {

    private final ProtectedVerificationSubmissionService protectedVerificationSubmissionService;
    private final FeaturesProperties featuresProperties;
    private final AuthSupport authSupport;

    @PostMapping
    @PreAuthorize("hasRoleForPartner('protectedverificationsubmission','write', #submission.partnerId())")
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
        description = "Not authorized or partner is not trusted. Possible error codes: RESOURCE_FORBIDDEN, PARTNER_IS_NOT_TRUSTED.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    public ProtectedVerificationSubmissionDto createProtectedVerificationSubmission(
        @Valid @RequestBody ProtectedVerificationSubmissionRequestDto submission
    ) {
        return protectedVerificationSubmissionService.createProtectedVerificationSubmission(submission);
    }

    @PreAuthorize("hasRole('protectedverificationsubmission','read')")
    @GetMapping("/categories")
    @Operation(summary = "Get all available ProtectedVerificationCategories")
    public List<ProtectedVerificationCategoryDto> getProtectedVerificationCategories() {
        return protectedVerificationSubmissionService.getProtectedVerificationCategories();
    }

    @PreAuthorize("hasRole('protectedverificationsubmission','read')")
    @GetMapping
    @Operation(summary = "Get all ProtectedVerificationSubmissions for partner.")
    @PageableAsQueryParam
    @SuppressWarnings("java:S1192") // ignore string literal duplication issue for auth checks
    public PagedModel<ProtectedVerificationSubmissionListItemDto> getProtectedVerificationSubmissions(
        @Valid @ParameterObject ProtectedVerificationSubmissionFilterDto filter,
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        if (authSupport.hasRoleForAllPartners("protectedverificationsubmission", "read")) {
            return new PagedModel<>(
                protectedVerificationSubmissionService.getAllProtectedVerificationSubmissions(filter, pageable)
            );
        } else {
            var authorizedPartnerIds = authSupport.getPartnerIdsForRole("protectedverificationsubmission", "read");
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
            return new PagedModel<>(
                protectedVerificationSubmissionService.getAllProtectedVerificationSubmissions(filter, pageable)
            );
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('protectedverificationsubmission','read')")
    @PostAuthorize("hasRoleForPartner('protectedverificationsubmission','read', returnObject.partnerId())")
    @Operation(summary = "Get specific ProtectedVerificationSubmission by ID")
    public ProtectedVerificationSubmissionDto getProtectedVerificationSubmission(@PathVariable @Valid UUID id) {
        return protectedVerificationSubmissionService.getProtectedVerificationSubmission(id);
    }
}
