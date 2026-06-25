package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionDoiValidationProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DeclarationOfIntentValidationException;
import ch.admin.suis.client.core.service.IValidationServiceClient;
import ch.admin.suis.client.core.service.to.FileRequest;
import ch.admin.suis.validator.rest.to.ValidStatus;
import ch.admin.suis.validator.rest.to.response.FileReport;
import ch.admin.suis.validator.rest.to.response.ValidationResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@AllArgsConstructor
public class DeclarationOfIntentValidator {

    /** Owner read/write only — no permissions for group or others. */
    private static final String TEMP_FILE_PERMISSIONS = "rw-------";

    private final TrustOnboardingSubmissionDoiValidationProperties properties;
    private final IValidationServiceClient validationServiceClient;

    public FileReport validateDeclarationOfIntent(MultipartFile file, SigningRule signingRule) {
        if (
            TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.NONE.equals(
                properties.mandant()
            )
        ) {
            return null;
        }

        Path tempFile = null;
        var violations = new ArrayList<DeclarationOfIntentValidatorErrorCodeDto>();
        try {
            tempFile = Files.createTempFile(
                "doi-upload-",
                ".pdf",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(TEMP_FILE_PERMISSIONS))
            );
            Files.write(tempFile, file.getBytes());
            var fileRequest = new FileRequest(tempFile.toFile(), properties.mandant().getValue());
            ValidationResponse validationResponse = validationServiceClient.validateOneRequest(
                // the list of files to validate
                List.of(fileRequest),
                // a flag specifying whether the validation service should generate and return a report in PDF format
                false,
                // the userOrganization property to pass to the validation service
                null,
                // the userOrganization property to pass to the validation service
                null,
                // the language property to pass to the validation service
                "de",
                // the pdfReportName property that should be echoed by the validation service
                null,
                //  a flag specifying whether the call should write details about the service input and results to the log
                false,
                // a flag specifying whether files not containing a signature should be processed by the validation service
                true
            );
            validateValidationResponse(violations, signingRule, validationResponse);
            if (!violations.isEmpty()) {
                throw new DeclarationOfIntentValidationException(
                    violations.stream().map(DeclarationOfIntentValidatorErrorCodeDto::toString).toList()
                );
            }
            var fileReports = validationResponse.getFileReports();
            return (fileReports != null && !fileReports.isEmpty()) ? fileReports.getFirst() : null;
        } catch (DeclarationOfIntentValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new DeclarationOfIntentValidationException(
                List.of(DeclarationOfIntentValidatorErrorCodeDto.VALIDATION_SERVICE_NOT_AVAILABLE.toString()),
                e
            );
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException _) {
                    // ignore — temp file cleanup failure does not affect validation result
                }
            }
        }
    }

    private void validateValidationResponse(
        List<DeclarationOfIntentValidatorErrorCodeDto> violations,
        SigningRule signingRule,
        ValidationResponse response
    ) {
        if (hasValidationResponseInvalidStatus(response)) {
            violations.add(DeclarationOfIntentValidatorErrorCodeDto.INVALID_SIGNATURES_FOR_MANDANT);
        }
        var fileReports = response.getFileReports();
        if (fileReports == null || fileReports.isEmpty()) {
            violations.add(DeclarationOfIntentValidatorErrorCodeDto.NO_SIGNATURES_FOUND);
            return;
        }
        var amountOfSignatures = fileReports.getFirst().getSignatureReports().size();
        if (amountOfSignatures == 0) {
            violations.add(DeclarationOfIntentValidatorErrorCodeDto.NO_SIGNATURES_FOUND);
            return;
        }

        if (
            signingRule == SigningRule.SINGLE_SIGNATURE &&
            amountOfSignatures != SigningRule.SINGLE_SIGNATURE.getRequiredSignatories()
        ) {
            violations.add(DeclarationOfIntentValidatorErrorCodeDto.VIOLATING_DOI_VARIANT_SINGLE_SIGNATURE);
        } else if (
            signingRule == SigningRule.JOINT_SIGNATURE_TWO &&
            amountOfSignatures != SigningRule.JOINT_SIGNATURE_TWO.getRequiredSignatories()
        ) {
            violations.add(DeclarationOfIntentValidatorErrorCodeDto.VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_TWO);
        } else if (
            signingRule == SigningRule.JOINT_SIGNATURE_THREE &&
            amountOfSignatures != SigningRule.JOINT_SIGNATURE_THREE.getRequiredSignatories()
        ) {
            violations.add(DeclarationOfIntentValidatorErrorCodeDto.VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_THREE);
        }
    }

    private boolean hasValidationResponseInvalidStatus(ValidationResponse response) {
        return response.isValid() == ValidStatus.INVALID || response.isValid() == ValidStatus.UNSURE;
    }
}
