package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema.VcSchemaSubmissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "VcSchemaSubmission", description = "VcSchemaSubmission API")
@RequestMapping("/api/v1/internal/trust")
public class VcSchemaSubmissionInternalController {

    private final VcSchemaSubmissionService service;

    @PreAuthorize("hasRole('vcschemasubmission', 'read')")
    @PostAuthorize("hasRoleForPartner('vcschemasubmission', 'read', returnObject.getPartnerId())")
    @GetMapping("/vc-schema-submissions/{id}")
    public VcSchemaSubmissionDto getVcSchemaSubmission(@PathVariable @Valid UUID id) {
        return service.getVcSchemaSubmission(id);
    }
}
