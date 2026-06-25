package ch.admin.bj.swiyu.core.business.modules.status.infrastructure.web;

import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/status")
@Tag(name = "Status B2B V2", description = "Status B2B API V2 — Enforces Swiss profile 1.0.0 conformity")
@RequiredArgsConstructor
class StatusB2BV2Controller {

    private final StatusListEntryService statusListEntryService;

    @PutMapping(
        value = "business-entities/{businessEntityId}/status-list-entries/{statusRegistryEntryId}",
        consumes = "application/statuslist+jwt"
    )
    @PreAuthorize("hasRoleForPartner('status', 'write', #businessEntityId)")
    @Operation(
        summary = "Upload a status list with Swiss profile conformity validation.",
        description = """
        Uploads a Token Status List JWT and validates Swiss profile conformity in addition to the base checks:
        - JWT header `typ` must be `statuslist+jwt`
        - JWT header `profile_version` must be `swiss-profile-vc:1.0.0`
        - `exp` claim must be set
        - Decompressed `lst` must not exceed 200KB
        - Decompressed bit count must be byte-aligned (size-in-bits % 8 = 0) and aligned with `bits`-per-entry
        """
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
            The JWT header must include `typ: statuslist+jwt` and `profile_version: swiss-profile-vc:1.0.0`.
            The payload must include an `exp` claim.
            """,
            content = @Content(
                examples = @ExampleObject(
                    "eyJraWQiOiJkaWQ6dGR3Oi4uLiIsInR5cCI6InN0YXR1c2xpc3Qrand0IiwiYWxnIjoiRVMyNTYiLCJwcm9maWxlX3ZlcnNpb24iOiJzd2lzcy1wcm9maWxlLXZjOjEuMC4wIn0.eyJpc3MiOiJkaWQ6dGR3Oi4uLiIsInN1YiI6Imh0dHBzOi8vc3RhdHVzLi4uL2FwaS92MS9zdGF0dXNsaXN0Ly4uLmp3dCIsImlhdCI6MTc0NzM5MjM1NCwiZXhwIjoxNzc4OTI4MzU0LCJzdGF0dXNfbGlzdCI6eyJsc3QiOiJlTnJ0elFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQSIsImJpdHMiOjJ9fQ.signature"
                )
            )
        ) String statusListVc
    ) {
        statusListEntryService.updateStatusListEntryV2(businessEntityId, statusRegistryEntryId, statusListVc);
    }
}
