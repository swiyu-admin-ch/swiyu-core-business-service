package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import ch.admin.bj.swiyu.core.business.common.api.ApiErrorCodeDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.demodata.DemoDataConstants;
import ch.admin.bj.swiyu.core.business.common.infrastructure.web.controller.RestExceptionMapper;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentListItemDto;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidationApiErrorDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentUploadRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DeclarationOfIntentValidationException;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "TrustOnboardingSubmission", description = "TrustOnboardingSubmission API")
@RequestMapping("/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document")
public class TrustOnboardingSubmissionDocumentsInternalController {

    private final TrustOnboardingService trustOnboardingService;
    private final PartnerDocumentService partnerDocumentService;
    private final AuthSupport authSupport;

    @PreAuthorize("hasRole('trustonboardingsubmission','read')")
    @GetMapping
    @Operation(summary = "List all documents for a TrustOnboardingSubmission")
    @PageableAsQueryParam
    public PagedModel<TrustOnboardingSubmissionDocumentListItemDto> listAllDocumentsForTrustOnboarding(
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable,
        @Schema(
            example = DemoDataConstants.TrustOnboardingSubmission.ID_SUCCEEDED
        ) @PathVariable @Valid UUID trustOnboardingSubmissionId
    ) {
        validateReadPermission(trustOnboardingSubmissionId);

        return new PagedModel<>(
            trustOnboardingService.findAllDocumentsByTrustOnboardingSubmissionId(trustOnboardingSubmissionId, pageable)
        );
    }

    @PreAuthorize("hasRole('trustonboardingsubmission','read')")
    @GetMapping("/{documentId}")
    @Operation(summary = "Get specific document for TrustOnboardingSubmission by ID")
    public TrustOnboardingSubmissionDocumentDto getDocumentForTrustOnboarding(
        @Schema(
            example = DemoDataConstants.TrustOnboardingSubmission.ID_SUCCEEDED
        ) @PathVariable @Valid UUID trustOnboardingSubmissionId,
        @PathVariable @Valid UUID documentId
    ) {
        validateReadPermission(trustOnboardingSubmissionId);
        return partnerDocumentService.getDocumentForTrustOnboardingSubmission(trustOnboardingSubmissionId, documentId);
    }

    @PreAuthorize("hasRole('trustonboardingsubmission','write')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document")
    @ApiResponse(responseCode = "200", description = "Document uploaded successfully")
    @ApiResponse(
        responseCode = "400",
        description = """
        Validation failed. For Declaration of Intent documents the errorCode will be \
        TRUST_ONBOARDING_DOCUMENT_VALIDATION_FAILED and additionalDetails contains the \
        specific DoI violation codes (see DeclarationOfIntentValidatorErrorCode). \
        For general document validation failures the errorCode will be DATA_INVALID.\
        """,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                oneOf = {
                    ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto.class,
                    DeclarationOfIntentValidationApiErrorDto.class,
                }
            )
        )
    )
    public TrustOnboardingSubmissionDocumentListItemDto uploadTrustOnboardingSubmissionDocument(
        @Schema(
            example = DemoDataConstants.TrustOnboardingSubmission.ID_UNSUBMITTED
        ) @PathVariable @Valid UUID trustOnboardingSubmissionId,
        @ModelAttribute @Valid TrustOnboardingSubmissionDocumentUploadRequestDto request
    ) {
        validateWritePermission(trustOnboardingSubmissionId);
        return trustOnboardingService.uploadTrustOnboardingSubmissionDocument(trustOnboardingSubmissionId, request);
    }

    @PreAuthorize("hasRole('trustonboardingsubmission','read')")
    @GetMapping("/doi")
    @Operation(summary = "Get specific document for TrustOnboardingSubmission by ID")
    public ResponseEntity<Resource> getDeclarationOfIntentDocumentForTrustOnboarding(
        @Schema(
            example = DemoDataConstants.TrustOnboardingSubmission.ID_UNSUBMITTED
        ) @PathVariable @Valid UUID trustOnboardingSubmissionId,
        @RequestParam("language") LanguageDto language
    ) {
        var partnerId = validateReadPermission(trustOnboardingSubmissionId);
        var filename = "declaration-of-intent-%s-%s-%s.pdf".formatted(
            partnerId,
            trustOnboardingSubmissionId,
            language.name().toLowerCase(Locale.ROOT)
        );

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(filename))
            .body(trustOnboardingService.getDeclarationOfIntentDocument(trustOnboardingSubmissionId, language));
    }

    @PreAuthorize("hasRole('trustonboardingsubmission','write')")
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a document from a TrustOnboardingSubmission")
    public void deleteTrustOnboardingSubmissionDocument(
        @Schema(
            example = DemoDataConstants.TrustOnboardingSubmission.ID_UNSUBMITTED
        ) @PathVariable @Valid UUID trustOnboardingSubmissionId,
        @PathVariable @Valid UUID documentId
    ) {
        validateWritePermission(trustOnboardingSubmissionId);
        trustOnboardingService.deleteTrustOnboardingSubmissionDocument(trustOnboardingSubmissionId, documentId);
    }

    @ExceptionHandler(DeclarationOfIntentValidationException.class)
    public ResponseEntity<DeclarationOfIntentValidationApiErrorDto> handleDeclarationOfIntentValidation(
        final DeclarationOfIntentValidationException e
    ) {
        log.info("Declaration of intent validation failed", e);
        var violationCodes = e
            .getAdditionalDetails()
            .stream()
            .map(DeclarationOfIntentValidatorErrorCodeDto::fromCode)
            .toList();
        ApiErrorCodeDto errorCode = RestExceptionMapper.toBusinessExceptionErrorCodeDto(e.getErrorCode());
        return new ResponseEntity<>(
            new DeclarationOfIntentValidationApiErrorDto(errorCode, e.getMessage(), violationCodes),
            HttpStatus.BAD_REQUEST
        );
    }

    private void validateWritePermission(UUID trustOnboardingSubmissionId) throws AccessDeniedException {
        var partnerId = trustOnboardingService.getTrustOnboardingSubmission(trustOnboardingSubmissionId).partnerId();
        authSupport.validateHasRoleForPartner("trustonboardingsubmission", "write", partnerId);
    }

    private UUID validateReadPermission(UUID trustOnboardingSubmissionId) throws AccessDeniedException {
        var partnerId = trustOnboardingService.getTrustOnboardingSubmission(trustOnboardingSubmissionId).partnerId();
        authSupport.validateHasRoleForPartner("trustonboardingsubmission", "read", partnerId);
        return partnerId;
    }
}
