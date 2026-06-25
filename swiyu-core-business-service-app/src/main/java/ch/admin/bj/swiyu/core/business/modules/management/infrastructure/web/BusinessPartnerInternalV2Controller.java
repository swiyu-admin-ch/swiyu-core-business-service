package ch.admin.bj.swiyu.core.business.modules.management.infrastructure.web;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerListItemDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreatePartnerDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = { "/api/v2/internal/management/business-partners/" })
@RequiredArgsConstructor
@Tag(name = "Business Partner V2", description = "Business Partner API")
class BusinessPartnerInternalV2Controller {

    private final BusinessPartnerService businessPartnerService;
    private final AuthSupport authSupport;

    @PreAuthorize("isAuthenticated()") // any user that is logged in can register an organization
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(
        responseCode = "400",
        description = "Business validation failed. Possible error codes: DATA_INVALID, BUSINESS_DATA_INTEGRITY_VIOLATION, OBJECT_COUNT_LIMIT_REACHED.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class)
        )
    )
    @ApiResponse(responseCode = "401", description = "No Authentication token provided")
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
    public BusinessPartnerDto createBusinessPartner(
        JeapAuthenticationToken token,
        @RequestBody @Valid CreatePartnerDto createPartnerDto
    ) {
        if (
            createPartnerDto.partnerType() == BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION &&
            !authSupport.isGovernmentalAllowlistUser()
        ) {
            throw new AccessDeniedException("A governmental institution can only be created by governmental users.");
        }
        return businessPartnerService.createBusinessPartnerV2(
            createPartnerDto,
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
    public PagedModel<BusinessPartnerListItemDto> getBusinessPartners(
        @Parameter(hidden = true) final Pageable pageable
    ) {
        if (authSupport.hasRoleForAllPartners("businesspartner", "read")) {
            return new PagedModel<>(businessPartnerService.getAllPartners(pageable));
        } else {
            var partnerIds = authSupport.getPartnerIdsForRole("businesspartner", "read");
            return new PagedModel<>(businessPartnerService.getAllPartnersById(partnerIds, pageable));
        }
    }

    @PreAuthorize("hasRoleForPartner('businesspartner','read',#id)")
    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Missing authorization token")
    @Operation(
        summary = "Get a business partner by ID that's associated with the user.",
        description = "Uses the bproles of the bearer token to identify owned roles"
    )
    public BusinessPartnerDto getBusinessPartner(@PathVariable @Valid UUID id) {
        return businessPartnerService.getBusinessPartner(id);
    }
}
