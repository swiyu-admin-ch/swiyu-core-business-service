package ch.admin.bj.swiyu.core.business.modules.management.infrastructure.web;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreateBusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.UpdateBusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @deprecated since 1.13.35. Use /api/v2/internal/management/business-partners instead
 */
@Slf4j
@RestController
@RequestMapping(
    value = {
        /* Mapping will be removed once portal-scs is migrated (EID-5379) */ "/api/v1/management/business-entities/",
        "/api/v1/internal/management/business-partners/",
    }
)
@RequiredArgsConstructor
@Tag(
    name = "Business Partner",
    description = "Business Partner API. Note: /api/v1/management/business-entities is deprecated and is replaced by /api/v1/internal/management/business-partners"
)
@SuppressWarnings("java:S1133") // warning about deprecation
@Deprecated(since = "1.13.35")
class BusinessPartnerInternalController {

    private final BusinessPartnerService businessPartnerService;
    private final AuthSupport authSupport;

    @PreAuthorize("isAuthenticated()") // any user that is logged in can register an organization
    @PostMapping("")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "No Authentication token provided")
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
    @Operation(
        summary = "Create a new business entity controlled by the signed in user. Only one business entity is allowed per user."
    )
    public BusinessEntityDto createBusinessEntity(
        JeapAuthenticationToken token,
        @RequestBody @Valid CreateBusinessEntityDto businessEntityDto
    ) {
        if (
            businessEntityDto.type() == BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION &&
            !authSupport.isGovernmentalAllowlistUser()
        ) {
            throw new AccessDeniedException("A governmental institution can only be created by governmental users.");
        }
        return businessPartnerService.createBusinessPartnerV1(
            businessEntityDto,
            token.getPreferredUsername().toUpperCase()
        );
    }

    @PreAuthorize("hasRole('businesspartner','read')")
    @GetMapping("")
    @PageableAsQueryParam
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Missing authorization token")
    @Operation(
        summary = "Gets all entities associated with the user.",
        description = "Uses the bproles of the bearer token to identify owned roles"
    )
    public PagedModel<BusinessEntityDto> getBusinessEntities(@Parameter(hidden = true) final Pageable pageable) {
        var partnerIds = authSupport.getPartnerIdsForRole("businesspartner", "read");
        return new PagedModel<>(businessPartnerService.getAllEntities(partnerIds, pageable));
    }

    @PreAuthorize("hasRoleForPartner('businesspartner', 'read', #id.toString())")
    @GetMapping("/{id}")
    public BusinessEntityDto getBusinessPartner(@PathVariable @Valid UUID id) {
        var businessPartner = businessPartnerService.getBusinessEntity(id);
        if (businessPartner.isEmpty()) {
            throw new ResourceNotFoundException(String.format("BusinessPartner with id '%s' not found.", id));
        }
        return businessPartner.get();
    }

    @PreAuthorize("hasRoleForPartner('businesspartner', 'write', #businessEntityId.toString())")
    @PutMapping("{businessEntityId}")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(
        responseCode = "400",
        description = "Business validation failed. Possible error codes: DATA_INVALID, BUSINESS_DATA_INTEGRITY_VIOLATION.",
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
        description = "Not found. Possible error codes: RESOURCE_NOT_FOUND.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    @Operation(summary = "Updates the business entity to the defined specifications.")
    public BusinessEntityDto updateBusinessEntity(
        @RequestBody @Valid UpdateBusinessEntityDto updateBusinessEntityDto,
        @PathVariable @Valid UUID businessEntityId
    ) {
        return businessPartnerService.updateBusinessEntity(businessEntityId, updateBusinessEntityDto);
    }

    @PreAuthorize("hasRoleForPartner('businesspartner', 'write', #businessEntityId.toString())")
    @DeleteMapping("{businessEntityId}")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "No Authentication token provided")
    @ApiResponse(responseCode = "403", description = "Nor Authorized to perform delete on specified entity")
    @Operation(summary = "Delete a new business entity controlled by the signed in user.")
    public void deleteBusinessEntity(@PathVariable @Valid UUID businessEntityId) {
        businessPartnerService.deleteBusinessEntity(businessEntityId);
    }
}
