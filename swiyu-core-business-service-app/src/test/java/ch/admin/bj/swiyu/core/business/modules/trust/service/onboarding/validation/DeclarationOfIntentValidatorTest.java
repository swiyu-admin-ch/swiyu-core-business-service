package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.INVALID_SIGNATURES_FOR_MANDANT;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.NO_SIGNATURES_FOUND;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.VALIDATION_SERVICE_NOT_AVAILABLE;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_TWO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionDoiValidationProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DeclarationOfIntentValidationException;
import ch.admin.suis.client.core.service.IValidationServiceClient;
import ch.admin.suis.validator.rest.to.ValidStatus;
import ch.admin.suis.validator.rest.to.response.FileReport;
import ch.admin.suis.validator.rest.to.response.SignatureReport;
import ch.admin.suis.validator.rest.to.response.ValidationResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DeclarationOfIntentValidatorTest {

    @Mock
    private IValidationServiceClient validationServiceClient;

    @Test
    void validateDeclarationOfIntent_rejects_when_no_signatures_found() throws Exception {
        var validator = createValidator(
            TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.SWISS_PKI
        );
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));
        when(
            validationServiceClient.validateOneRequest(
                anyList(),
                eq(false),
                isNull(),
                isNull(),
                eq("de"),
                isNull(),
                eq(false),
                eq(true)
            )
        ).thenReturn(emptyFileReportResponse());

        assertThatThrownBy(() -> validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE))
            .isInstanceOf(DeclarationOfIntentValidationException.class)
            .satisfies(e ->
                assertThat(((DeclarationOfIntentValidationException) e).getAdditionalDetails()).contains(
                    NO_SIGNATURES_FOUND.toString()
                )
            );
    }

    @Test
    void validateDeclarationOfIntent_rejects_when_mandant_status_is_invalid() throws Exception {
        var validator = createValidator(
            TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.SWISS_PKI
        );
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));
        var response = mock(ValidationResponse.class);
        var fileReport = new FileReport();
        fileReport.setSignatureReports(List.of(new SignatureReport()));
        when(response.isValid()).thenReturn(ValidStatus.INVALID);
        when(response.getFileReports()).thenReturn(List.of(fileReport));
        when(
            validationServiceClient.validateOneRequest(
                anyList(),
                eq(false),
                isNull(),
                isNull(),
                eq("de"),
                isNull(),
                eq(false),
                eq(true)
            )
        ).thenReturn(response);

        assertThatThrownBy(() -> validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE))
            .isInstanceOf(DeclarationOfIntentValidationException.class)
            .satisfies(e ->
                assertThat(((DeclarationOfIntentValidationException) e).getAdditionalDetails()).contains(
                    INVALID_SIGNATURES_FOR_MANDANT.toString()
                )
            );
    }

    @Test
    void validateDeclarationOfIntent_rejects_when_signing_rule_and_signature_count_do_not_match() throws Exception {
        var validator = createValidator(
            TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.SWISS_PKI
        );
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));
        var response = mock(ValidationResponse.class);
        var fileReport = mock(FileReport.class);
        when(response.isValid()).thenReturn(ValidStatus.VALID);
        when(fileReport.getSignatureReports()).thenReturn(List.of(mock(SignatureReport.class)));
        when(response.getFileReports()).thenReturn(List.of(fileReport));
        when(
            validationServiceClient.validateOneRequest(
                anyList(),
                eq(false),
                isNull(),
                isNull(),
                eq("de"),
                isNull(),
                eq(false),
                eq(true)
            )
        ).thenReturn(response);

        assertThatThrownBy(() -> validator.validateDeclarationOfIntent(file, SigningRule.JOINT_SIGNATURE_TWO))
            .isInstanceOf(DeclarationOfIntentValidationException.class)
            .satisfies(e ->
                assertThat(((DeclarationOfIntentValidationException) e).getAdditionalDetails()).contains(
                    VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_TWO.toString()
                )
            );
    }

    @Test
    void validateDeclarationOfIntent_skips_remote_validation_when_mandant_is_none() {
        var validator = createValidator(
            TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.NONE
        );
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));

        var result = validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE);

        assertNull(result);
        verifyNoInteractions(validationServiceClient);
    }

    @Test
    void validateDeclarationOfIntent_wraps_validator_client_exceptions() throws Exception {
        var validator = createValidator(
            TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.SWISS_PKI
        );
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));
        when(
            validationServiceClient.validateOneRequest(
                anyList(),
                eq(false),
                isNull(),
                isNull(),
                eq("de"),
                isNull(),
                eq(false),
                eq(true)
            )
        ).thenThrow(new RuntimeException("validator unavailable"));

        assertThatThrownBy(() -> validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE))
            .isInstanceOf(DeclarationOfIntentValidationException.class)
            .satisfies(e ->
                assertThat(((DeclarationOfIntentValidationException) e).getAdditionalDetails()).contains(
                    VALIDATION_SERVICE_NOT_AVAILABLE.toString()
                )
            );
    }

    @Test
    void validateDeclarationOfIntent_keeps_uploaded_file_bytes_available_after_validation() throws Exception {
        var validator = createValidator(
            TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.SWISS_PKI
        );
        var originalBytes = "test-pdf-content".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", originalBytes);
        var response = mock(ValidationResponse.class);
        var fileReport = mock(FileReport.class);
        when(response.isValid()).thenReturn(ValidStatus.VALID);
        when(response.getFileReports()).thenReturn(List.of(fileReport));
        when(fileReport.getSignatureReports()).thenReturn(List.of(mock(SignatureReport.class)));
        when(
            validationServiceClient.validateOneRequest(
                anyList(),
                eq(false),
                isNull(),
                isNull(),
                eq("de"),
                isNull(),
                eq(false),
                eq(true)
            )
        ).thenReturn(response);

        validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE);

        assertThat(file.getBytes()).isEqualTo(originalBytes);
    }

    private DeclarationOfIntentValidator createValidator(
        TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties mandant
    ) {
        return new DeclarationOfIntentValidator(
            new TrustOnboardingSubmissionDoiValidationProperties(
                mandant,
                new TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationDiscreteValidatorServiceProperties(
                    "http://validator.local",
                    "user",
                    "password"
                )
            ),
            validationServiceClient
        );
    }

    private ValidationResponse emptyFileReportResponse() {
        var fileReport = new FileReport();
        fileReport.setSignatureReports(List.of());
        var response = new ValidationResponse();
        response.setFileReports(List.of(fileReport));
        return response;
    }
}
