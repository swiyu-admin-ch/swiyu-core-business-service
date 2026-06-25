package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import ch.admin.bj.swiyu.core.business.common.exceptions.VirusDetectedException;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentValidatorErrorCodeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.Errors;
import org.springframework.validation.SimpleErrors;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustOnboardingSubmissionDocumentValidator {

    private final TrustOnboardingSubmissionLimitProperties trustOnboardingSubmissionLimitProperties;
    private final PartnerDocumentService partnerDocumentService;
    private final TrustOnboardingSubmissionValidator trustOnboardingSubmissionValidator;

    public Errors validateDocument(
        TrustOnboardingSubmission trustOnboardingSubmission,
        MultipartFile file,
        @Nullable Errors errors
    ) throws VirusDetectedException {
        if (errors == null) {
            errors = new SimpleErrors(file);
        }

        trustOnboardingSubmissionValidator.validateTrustOnboardingSubmissionCanBeEdited(
            trustOnboardingSubmission,
            errors
        );
        validateDocumentContentType(file, errors);
        validateDocumentSize(file, errors);
        validateStorageCapacity(trustOnboardingSubmission, errors);

        return errors;
    }

    private void validateDocumentContentType(MultipartFile file, Errors errors) {
        if (
            !trustOnboardingSubmissionLimitProperties.documents().allowedContentTypes().contains(file.getContentType())
        ) {
            errors.reject(
                TrustOnboardingSubmissionDocumentValidatorErrorCodeDto.WRONG_CONTENT_TYPE.toString(),
                "File has content type '%s' but one of '%s' is required.".formatted(
                    file.getContentType(),
                    trustOnboardingSubmissionLimitProperties.documents().allowedContentTypes()
                )
            );
        }
    }

    private void validateStorageCapacity(TrustOnboardingSubmission trustOnboardingSubmission, Errors errors) {
        var currentCountBySubmission = partnerDocumentService.countPartnerDocumentsByTrustOnboardingSubmissionId(
            trustOnboardingSubmission.getId()
        );
        var currentCountByBusinessPartner = partnerDocumentService.countPartnerDocumentsByPartnerId(
            trustOnboardingSubmission.getPartnerId()
        );

        if (
            currentCountBySubmission >
            trustOnboardingSubmissionLimitProperties.documents().defaultMaxCountPerSubmission()
        ) {
            errors.reject(
                TrustOnboardingSubmissionDocumentValidatorErrorCodeDto.STORAGE_CAPACITY_EXCEEDED.toString(),
                "There are already %d documents attached to this submission. You cannot upload any more documents.".formatted(
                    trustOnboardingSubmissionLimitProperties.documents().defaultMaxCountPerSubmission()
                )
            );
        }

        if (
            currentCountByBusinessPartner >
            trustOnboardingSubmissionLimitProperties.documents().defaultMaxCountPerBusinessPartner()
        ) {
            errors.reject(
                TrustOnboardingSubmissionDocumentValidatorErrorCodeDto.STORAGE_CAPACITY_EXCEEDED.toString(),
                "There are already %d documents attached to this business partner. You cannot upload any more documents.".formatted(
                    trustOnboardingSubmissionLimitProperties.documents().defaultMaxCountPerBusinessPartner()
                )
            );
        }
    }

    private void validateDocumentSize(MultipartFile file, Errors errors) {
        var fileSize = file.getSize();
        if (fileSize > trustOnboardingSubmissionLimitProperties.documents().maxFileSize().toBytes()) {
            errors.reject(
                TrustOnboardingSubmissionDocumentValidatorErrorCodeDto.FILE_SIZE.toString(),
                "File has size %s but cannot exceed %s.".formatted(
                    DataSize.ofBytes(fileSize),
                    trustOnboardingSubmissionLimitProperties.documents().maxFileSize()
                )
            );
        }
        if (fileSize < trustOnboardingSubmissionLimitProperties.documents().minFileSize().toBytes()) {
            errors.reject(
                TrustOnboardingSubmissionDocumentValidatorErrorCodeDto.FILE_SIZE.toString(),
                "File has size %s but cannot exceed %s.".formatted(
                    DataSize.ofBytes(fileSize),
                    trustOnboardingSubmissionLimitProperties.documents().minFileSize()
                )
            );
        }
    }
}
