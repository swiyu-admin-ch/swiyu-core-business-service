package ch.admin.bj.swiyu.core.business.modules.identifier.infrastructure.web;

import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryFilterDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/api/v1/identifier")
@Tag(name = "Identifier B2B", description = "Identifier B2B API")
@RequiredArgsConstructor
class IdentifierB2BController {

    private final IdentifierEntryService identifierEntryService;

    @GetMapping(value = "business-entities/{partnerId}/identifier/")
    @PreAuthorize("hasRoleForPartner('identifier', 'read', #partnerId)")
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
                IdentifierEntryFilterDto.builder().businessPartnerId(partnerId).build(),
                pageable
            )
        );
    }

    @PostMapping(value = "business-entities/{partnerId}/identifier-entries/")
    @PreAuthorize("hasRoleForPartner('identifier', 'write', #partnerId)")
    @Operation(summary = "Create a new entry on the identifier registry to store a did:tdw including key material.")
    public IdentifierEntryDto createIdentifierEntry(
        @PathVariable @Parameter(
            description = "The business partner id provided by the self service api.",
            example = "8432e1f3-8119-4fb9-a879-190ab2cb9deb"
        ) @Valid UUID partnerId
    ) {
        return identifierEntryService.createIdentifierEntry(partnerId);
    }

    @PutMapping(
        value = "business-entities/{partnerId}/identifier-entries/{identifierRegistryEntryId}",
        consumes = "application/jsonl+json"
    )
    @PreAuthorize("hasRoleForPartner('identifier', 'write', #partnerId)")
    @Operation(summary = "Update the entry on the identifier registry to store an updated did:tdw.")
    public void updateIdentifierEntry(
        @PathVariable @Parameter(
            description = "The business partner id provided by the self service api.",
            example = "8432e1f3-8119-4fb9-a879-190ab2cb9deb"
        ) @Valid UUID partnerId,
        @Valid @PathVariable @Parameter(
            description = "The identifier registry entry ID provided by /api/v1/identifier-entries/",
            example = "18fa7c77-9dd1-4e20-a147-fb1bec146085"
        ) UUID identifierRegistryEntryId,
        @RequestBody(required = true) @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content
        ) String identifierDidJsonl
    ) {
        identifierEntryService.updateIdentifierEntry(partnerId, identifierRegistryEntryId, identifierDidJsonl);
    }
}
