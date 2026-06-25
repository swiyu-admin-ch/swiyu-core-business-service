package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.trust.api.CreateVcMetadataTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema.VcSchemaSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ConditionalOnProperty(prefix = "features", value = "EIDARTFE_754_VC_SCHEMA", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "VcSchemaSubmission B2B", description = "VcSchemaSubmission B2B API")
@RequestMapping("/api/v1/trust")
public class VcSchemaSubmissionB2BController {

    private final VcSchemaSubmissionService service;
    private final AuthSupport authSupport;

    @PreAuthorize("hasRole('vcschemasubmission', 'write')")
    @PostAuthorize("hasRoleForPartner('vcschemasubmission', 'write', returnObject.getPartnerId())")
    @PostMapping("/vc-schema-submissions")
    public VcSchemaSubmissionDto createVcSchemaSubmission(@Valid @RequestBody CreateVcMetadataTypeDto request) {
        var partnerId = authSupport.getPartnerIdForRole("vcschemasubmission", "write");
        return service.createVcSchemaSubmission(request, partnerId);
    }

    @PreAuthorize("hasRole('vcschemasubmission', 'read')")
    @PostAuthorize(
        "@authSupport.hasRoleForPartners('vcschemasubmission', 'read', returnObject.getContent().![partnerId])"
    )
    @GetMapping("/vc-schema-submissions")
    @Operation(summary = "Get all VcSchemaSubmissions for partner.")
    @PageableAsQueryParam
    public Page<VcSchemaSubmissionDto> getVcSchemaSubmissions(
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        var partnerId = authSupport.getPartnerIdForRole("vcschemasubmission", "read");
        return service.getAllEntities(partnerId, pageable);
    }

    @PreAuthorize("hasRole('vcschemasubmission', 'read')")
    @PostAuthorize("hasRoleForPartner('vcschemasubmission', 'read', returnObject.getPartnerId())")
    @GetMapping("/vc-schema-submissions/{id}")
    public VcSchemaSubmissionDto getVcSchemaSubmission(@PathVariable @Valid UUID id) {
        return service.getVcSchemaSubmission(id);
    }
}
