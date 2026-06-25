package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionInternalDto;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vqps.VqpsSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "VqpsSubmission Internal", description = "VqpsSubmission Internal API")
@RequestMapping("/api/v1/internal/trust")
public class VqpsSubmissionInternalController {

    private final VqpsSubmissionService vqpsSubmissionService;

    @PreAuthorize("hasRole('vqpssubmission', 'read')")
    @Operation(summary = "Get a VqpsSubmission with full submission data")
    @GetMapping("/vqps-submissions/{id}")
    public VqpsSubmissionInternalDto getVqpsSubmission(@PathVariable @Valid UUID id) {
        return vqpsSubmissionService.getVqpsSubmissionInternal(id);
    }
}
