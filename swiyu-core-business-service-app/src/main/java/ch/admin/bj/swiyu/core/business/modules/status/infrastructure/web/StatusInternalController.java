package ch.admin.bj.swiyu.core.business.modules.status.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.demodata.DemoDataConstants;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryLimitsDto;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/status")
@Tag(name = "Status", description = "Status API")
@RequiredArgsConstructor
public class StatusInternalController {

    private final StatusListEntryService statusListEntryService;

    @GetMapping(value = "business-partner/{businessPartnerId}/limits")
    @PreAuthorize("hasRoleForPartner('status', 'read', #businessPartnerId)")
    @Operation(summary = "Get current limits in the scope of the status registry.")
    public StatusListEntryLimitsDto getStatusListEntryLimits(
        @PathVariable @Parameter(
            description = "The business partner id to get limits for",
            example = DemoDataConstants.BusinessPartner.CORE_ID_BP_DEFAULT
        ) @Valid UUID businessPartnerId
    ) {
        return statusListEntryService.getLimits(businessPartnerId);
    }
}
