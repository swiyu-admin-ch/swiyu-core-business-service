package ch.admin.bj.swiyu.core.business.modules.status.infrastructure.web;

import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryCreationDto;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryDto;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/status")
@Tag(name = "Status B2B", description = "Status B2B API")
@RequiredArgsConstructor
class StatusB2BController {

    private final StatusListEntryService statusListEntryService;
    private final BusinessPartnerService businessPartnerService;

    @PostMapping(value = "business-entities/{businessEntityId}/status-list-entries/")
    @PreAuthorize("hasRoleForPartner('status', 'write', #businessEntityId)")
    @Operation(summary = "Create a new entry on the status registry to store a status list.")
    public StatusListEntryCreationDto createStatusListEntry(
        @PathVariable @Parameter(
            description = "The business partner id provided by the self service api.",
            example = "8432e1f3-8119-4fb9-a879-190ab2cb9deb"
        ) @Valid UUID businessEntityId
    ) {
        businessPartnerService.validateBusinessPartnerExists(businessEntityId);
        return statusListEntryService.createStatusListEntry(businessEntityId);
    }

    @GetMapping(value = "business-entities/{businessEntityId}/status-list-entries/")
    @PreAuthorize("hasRoleForPartner('status', 'read', #businessEntityId)")
    @Operation(summary = "Get all entries for the status registry.")
    @PageableAsQueryParam
    public Page<StatusListEntryDto> getAllStatusListEntries(
        @PathVariable @Parameter(
            description = "The business partner id provided by the self service api.",
            example = "8432e1f3-8119-4fb9-a879-190ab2cb9deb"
        ) @Valid UUID businessEntityId,
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return statusListEntryService.getPagedByBusinessPartner(businessEntityId, pageable);
    }

    /**
     * @deprecated Use /api/v2/status/... instead. This endpoint does not enforce Swiss profile conformity.
     */
    @Deprecated(since = "3.25.0", forRemoval = true)
    @SuppressWarnings("java:S1133")
    @PutMapping(
        value = "business-entities/{businessEntityId}/status-list-entries/{statusRegistryEntryId}",
        consumes = "application/statuslist+jwt"
    )
    @PreAuthorize("hasRoleForPartner('status', 'write', #businessEntityId)")
    @Operation(
        summary = "Upload a status list to the status registry.",
        deprecated = true,
        description = "Deprecated: Use /api/v2/status/... instead. This endpoint does not enforce Swiss profile conformity."
    )
    public void updateStatusListEntry(
        @PathVariable @Parameter(
            description = "The business partner id provided by the self service api.",
            example = "8432e1f3-8119-4fb9-a879-190ab2cb9deb"
        ) @Valid UUID businessEntityId,
        @Valid @PathVariable @Parameter(
            description = "The status registry entry ID provided by /api/v1/status-list-entries/",
            example = "18fa7c77-9dd1-4e20-a147-fb1bec146085"
        ) UUID statusRegistryEntryId,
        @RequestBody(required = true) @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = """
            Token status list VC according to spec:
            https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-02.html#name-status-list-request
            """,
            content = @Content(
                examples = @ExampleObject(
                    "eyJraWQiOiJkaWQ6dGR3OmhidGRveWp0bW15d2duanhtdTJkaW56eGdleWRremRjbWkzZGNtYnJnanNkaXl6emdxNHRlb2RjZ3l5ZG16ZGRtcXp0Z29qcmdpNGdleWp0bXkyZ2duanRndnJ3Z3l0Y2hleXdrenE9OmlkZW50aWZpZXItZGF0YS1zZXJ2aWNlLWQuYml0LmFkbWluLmNoOmFwaTp2MTpkaWQ6OWRjOWJiYTQtNWRiYS00ZjYwLWFkOWUtMjI2MDM4MzQzZjg5I2tleS0wMSIsInR5cCI6InN0YXR1c2xpc3Qrand0IiwiYWxnIjoiRVMyNTYifQ.eyJpc3MiOiJkaWQ6dGR3OmhidGRveWp0bW15d2duanhtdTJkaW56eGdleWRremRjbWkzZGNtYnJnanNkaXl6emdxNHRlb2RjZ3l5ZG16ZGRtcXp0Z29qcmdpNGdleWp0bXkyZ2duanRndnJ3Z3l0Y2hleXdrenE9OmlkZW50aWZpZXItZGF0YS1zZXJ2aWNlLWQuYml0LmFkbWluLmNoOmFwaTp2MTpkaWQ6OWRjOWJiYTQtNWRiYS00ZjYwLWFkOWUtMjI2MDM4MzQzZjg5Iiwic3ViIjoiaHR0cHM6Ly9zdGF0dXMtZGF0YS1zZXJ2aWNlLWQuYXBwcy5wLXN6Yi1yb3Mtc2hyZC1ucHItMDEuY2xvdWQuYWRtaW4uY2gvYXBpL3YxL3N0YXR1c2xpc3QvMDVkMmUwOWYtMjFkYy00Njk5LTg3OGYtODlhOGEyMjIyYzY3Lmp3dCIsImlhdCI6MTcyOTU4NzcwNiwic3RhdHVzX2xpc3QiOnsiYml0cyI6MiwibHN0IjoiZU5wallCalJBQUFBX3dBQiJ9fQ.1G_3ue2EGVXTwculW2RHe_9K3q3NWfOfkerCPu0cs7O4jHcd_2LGi83GscLZ3w9iGyYpZvnQYlr1PJ0AQoEz0w"
                )
            )
        ) String statusListVc
    ) {
        statusListEntryService.updateStatusListEntry(businessEntityId, statusRegistryEntryId, statusListVc);
    }
}
