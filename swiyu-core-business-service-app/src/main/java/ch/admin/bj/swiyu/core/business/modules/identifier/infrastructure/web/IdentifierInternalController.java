package ch.admin.bj.swiyu.core.business.modules.identifier.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.api.IdentifierUpdateRequestDto;
import ch.admin.bj.swiyu.core.business.common.demodata.DemoDataConstants;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryFilterDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryLimitsDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/identifier")
@Tag(name = "Identifier", description = "Identifier API")
@RequiredArgsConstructor
public class IdentifierInternalController {

    private final IdentifierEntryService identifierEntryService;

    @GetMapping(value = "business-entities/{partnerId}/limits")
    @PreAuthorize("hasRoleForPartner('identifier', 'read', #partnerId)")
    @Operation(summary = "Get current limits in the scope of the identifier registry.")
    public IdentifierEntryLimitsDto getIdentifierEntryLimits(
        @PathVariable @Parameter(
            description = "The business partner id to get limits for",
            example = DemoDataConstants.BusinessPartner.CORE_ID_BP_DEFAULT
        ) @Valid UUID partnerId
    ) {
        return identifierEntryService.getLimits(partnerId);
    }

    @GetMapping(value = "business-entities/{partnerId}/identifier/")
    @PreAuthorize("hasRoleForPartner('identifier', 'read', #partnerId)")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(
        responseCode = "400",
        description = "Bad request. Possible error codes: DATA_INVALID, INVALID_PAGINATION.",
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
    @Operation(summary = "Get all entries for the identifier registry.")
    @PageableAsQueryParam
    public PagedModel<IdentifierEntryDto> getAllIdentifierEntries(
        @PathVariable @Parameter(
            description = "The business partner id provided by the self service api.",
            example = "8432e1f3-8119-4fb9-a879-190ab2cb9deb"
        ) @NotNull @Valid UUID partnerId,
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(
            identifierEntryService.searchIdentifierEntries(
                IdentifierEntryFilterDto.builder().businessPartnerId(partnerId).activeOnly(true).build(),
                pageable
            )
        );
    }

    @GetMapping(value = "business-entities/{partnerId}/identifier/{identifierId}")
    @PreAuthorize("hasRoleForPartner('identifier', 'read', #partnerId)")
    @Operation(summary = "Get all entries for the identifier registry.")
    public IdentifierEntryDto getIdentifierEntry(
        @PathVariable @Parameter(
            description = "The business partner id provided by the self service api.",
            example = "8432e1f3-8119-4fb9-a879-190ab2cb9deb"
        ) @NotNull @Valid UUID partnerId,
        @PathVariable @Parameter(
            description = "The identifier entry id.",
            example = "d290f1ee-6c54-4b01-90e6-d701748f0851"
        ) @NotNull @Valid UUID identifierId
    ) {
        return identifierEntryService.getIdentifierEntry(partnerId, identifierId);
    }

    @PostMapping("/{partnerId}/identifier/{identifierId}/description")
    @PreAuthorize("hasRoleForPartner('identifier', 'write', #partnerId)")
    @Operation(summary = "Update description of a specific identifier entry of partner")
    public void updateIdentifierDescription(
        @PathVariable UUID partnerId,
        @PathVariable UUID identifierId,
        @Valid @RequestBody IdentifierUpdateRequestDto updateRequestDto
    ) {
        this.identifierEntryService.updateIdentifierEntryDescription(partnerId, identifierId, updateRequestDto);
    }
}
