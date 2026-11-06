package ch.admin.bj.swiyu.core.business.modules.management.infrastructure.web;

import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Manual DevOps triggers for the event-driven business partner sync (EID-6988): re-publish the
 * TiBusinessPartnerUpdatedEvent without state change, e.g. to recover from a dropped event.
 */
@RestController
@RequestMapping(value = { "/api/v1/internal/devops/business-partners" })
@RequiredArgsConstructor
@Tag(name = "DevOps", description = "Manual triggers for the event-driven business partner sync")
class DevopsInternalController {

    private final BusinessPartnerService businessPartnerService;

    @PreAuthorize("hasRoleForAllPartners('businesspartner', 'sync')")
    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Events published for all business partners")
    @ApiResponse(responseCode = "401", description = "Missing authorization token")
    @ApiResponse(responseCode = "403", description = "Not authorized")
    @Operation(
        summary = "Re-publish the TiBusinessPartnerUpdatedEvent for all business partners.",
        description = "Idempotent, no state change. Forces a full re-sync of the TMS partner copy."
    )
    public void syncAllBusinessPartners() {
        businessPartnerService.publishAllBusinessPartnerUpdatedEvents();
    }

    @PreAuthorize("hasRoleForPartner('businesspartner', 'sync', #businessPartnerId.toString())")
    @PostMapping("/sync/{businessPartnerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Event published for the business partner")
    @ApiResponse(responseCode = "401", description = "Missing authorization token")
    @ApiResponse(responseCode = "403", description = "Not authorized")
    @ApiResponse(responseCode = "404", description = "Business partner not found")
    @Operation(
        summary = "Re-publish the TiBusinessPartnerUpdatedEvent for one business partner.",
        description = "Idempotent, no state change. E.g. to recover from a dropped event."
    )
    public void syncBusinessPartner(@PathVariable @Valid UUID businessPartnerId) {
        businessPartnerService.publishBusinessPartnerUpdatedEvent(businessPartnerId);
    }
}
