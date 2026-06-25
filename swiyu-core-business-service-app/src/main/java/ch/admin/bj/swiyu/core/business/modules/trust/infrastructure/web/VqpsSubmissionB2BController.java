package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.exceptions.VqpsPublicationFailedException;
import ch.admin.bj.swiyu.core.business.common.exceptions.VqpsPublicationTimeoutException;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionB2BDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionCreateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vqps.VqpsPublicationAwaiter;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vqps.VqpsSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ConditionalOnProperty(prefix = "features", value = "EIDARTFE_754_VC_SCHEMA", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "VqpsSubmission B2B", description = "Verification Query Public Statement Submission B2B API")
@RequestMapping("/api/v1/trust")
public class VqpsSubmissionB2BController {

    private final AuthSupport authSupport;
    private final VqpsPublicationAwaiter publicationAwaiter;
    private final VqpsSubmissionService vqpsSubmissionService;

    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('vqpssubmission', 'write')")
    @PostAuthorize("hasRoleForPartner('vqpssubmission', 'write', returnObject.getPartnerId())")
    @Operation(
        summary = "Request a new VQPS publication into the Trust Registry by submitting a VqpsSubmission and " +
            "waits for the publication into the trust registry. "
    )
    @PostMapping("/vqps-submissions")
    public VqpsSubmissionB2BDto createVqpsSubmission(@Valid @RequestBody VqpsSubmissionCreateRequestDto request) {
        var partnerId = authSupport.getPartnerIdForRole("vqpssubmission", "write");
        var submission = vqpsSubmissionService.createVqpsSubmission(request, partnerId);
        log.debug(
            "Created VqpsSubmission with id {} for partnerId {}. Now waiting for publication...",
            submission.id(),
            partnerId
        );
        if (request.waitForPublication()) {
            try {
                return publicationAwaiter.waitForVqpsPublication(submission.id());
            } catch (VqpsPublicationTimeoutException e) {
                log.debug("Publication of VQPS with submission id {} timed out", submission.id(), e);
            }
        }
        return getVqpsSubmission(submission.id());
    }

    @PreAuthorize("hasRole('vqpssubmission', 'read')")
    @PostAuthorize("hasRoleForPartner('vqpssubmission', 'read', returnObject.getPartnerId())")
    @Operation(summary = "Get a VqpsSubmission by id")
    @GetMapping("/vqps-submissions/{id}")
    public VqpsSubmissionB2BDto getVqpsSubmission(@PathVariable @Valid UUID id) {
        return vqpsSubmissionService.getVqpsSubmissionB2B(id);
    }

    @PreAuthorize("hasRole('vqpssubmission', 'read')")
    @PostAuthorize("@authSupport.hasRoleForPartners('vqpssubmission', 'read', returnObject.getContent().![partnerId])")
    @GetMapping("/vqps-submissions")
    @Operation(summary = "Get all VqpsSubmissions for a partner")
    @PageableAsQueryParam
    public PagedModel<VqpsSubmissionB2BDto> getVqpsSubmissions(
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        var partnerId = authSupport.getPartnerIdForRole("vqpssubmission", "read");
        return new PagedModel<>(vqpsSubmissionService.getVqpsSubmissionsB2B(partnerId, pageable));
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(VqpsPublicationFailedException.class)
    public VqpsSubmissionB2BDto handlePublicationFailedException(final VqpsPublicationFailedException e) {
        log.debug("Failed to publish vqps", e);
        return vqpsSubmissionService.getVqpsSubmissionB2B(e.getVqpsSubmissionId());
    }
}
