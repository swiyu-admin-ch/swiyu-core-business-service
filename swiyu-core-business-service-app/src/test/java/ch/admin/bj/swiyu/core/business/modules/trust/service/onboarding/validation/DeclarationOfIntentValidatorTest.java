package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.INVALID_SIGNATURES_FOR_MANDANT;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.NO_SIGNATURES_FOUND;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.VALIDATION_SERVICE_NOT_AVAILABLE;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_TWO;
import static ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.NONE;
import static ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties.SWISS_PKI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.core.business.modules.trust.config.DiscreteValidatorClientConfig;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionDoiValidationProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DeclarationOfIntentValidationException;
import ch.admin.bj.swiyu.discrete.validator.DiscreteValidationResult;
import ch.admin.bj.swiyu.discrete.validator.DiscreteValidatorClient;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DeclarationOfIntentValidatorTest {

    @Mock
    private DiscreteValidatorClient discreteValidatorClient;

    @Test
    void validateDeclarationOfIntent_rejects_when_no_signatures_found() {
        var validator = createValidator(SWISS_PKI);
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));
        when(discreteValidatorClient.executeValidationRequest(any(), any(), anyInt())).thenReturn(
            emptyDiscreteValidationResult()
        );

        assertThatThrownBy(() -> validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE))
            .isInstanceOf(DeclarationOfIntentValidationException.class)
            .satisfies(e ->
                assertThat(((DeclarationOfIntentValidationException) e).getAdditionalDetails()).contains(
                    NO_SIGNATURES_FOUND.toString()
                )
            );
    }

    @Test
    void validateDeclarationOfIntent_rejects_when_mandant_status_is_invalid() {
        var validator = createValidator(SWISS_PKI);
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));
        when(discreteValidatorClient.executeValidationRequest(any(), any(), anyInt())).thenReturn(
            invalidDiscreteValidationResult()
        );

        assertThatThrownBy(() -> validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE))
            .isInstanceOf(DeclarationOfIntentValidationException.class)
            .satisfies(e ->
                assertThat(((DeclarationOfIntentValidationException) e).getAdditionalDetails()).contains(
                    INVALID_SIGNATURES_FOR_MANDANT.toString()
                )
            );
    }

    @Test
    void validateDeclarationOfIntent_rejects_when_signing_rule_and_signature_count_do_not_match() {
        var validator = createValidator(SWISS_PKI);
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));
        when(discreteValidatorClient.executeValidationRequest(any(), any(), anyInt())).thenReturn(
            validDiscreteValidationResult()
        );

        assertThatThrownBy(() -> validator.validateDeclarationOfIntent(file, SigningRule.JOINT_SIGNATURE_TWO))
            .isInstanceOf(DeclarationOfIntentValidationException.class)
            .satisfies(e ->
                assertThat(((DeclarationOfIntentValidationException) e).getAdditionalDetails()).contains(
                    VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_TWO.toString()
                )
            );
    }

    @Test
    void validateDeclarationOfIntent_validates_positive_when_mandant_is_none() {
        // note: we are creating the validator here with the discreteValidatorClientMock which
        // is used in app when mandant was configured with NONE
        var validator = createValidator(
            new DiscreteValidatorClientConfig(null, null).discreteValidatorClientMock(),
            NONE
        );
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));
        var result = validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE);
        assertNotNull(result.fileReport());
    }

    @Test
    void validateDeclarationOfIntent_wraps_validator_client_exceptions() {
        var validator = createValidator(SWISS_PKI);
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", "test".getBytes(StandardCharsets.UTF_8));

        when(discreteValidatorClient.executeValidationRequest(any(), any(), anyInt())).thenThrow(
            new IllegalStateException("validator unavailable")
        );

        assertThatThrownBy(() -> validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE))
            .isInstanceOf(DeclarationOfIntentValidationException.class)
            .satisfies(e ->
                assertThat(((DeclarationOfIntentValidationException) e).getAdditionalDetails()).contains(
                    VALIDATION_SERVICE_NOT_AVAILABLE.toString()
                )
            );
    }

    @Test
    void validateDeclarationOfIntent_keeps_uploaded_file_bytes_available_after_validation() throws IOException {
        var validator = createValidator(SWISS_PKI);
        var originalBytes = "test-pdf-content".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "doi.pdf", "application/pdf", originalBytes);
        when(discreteValidatorClient.executeValidationRequest(any(), any(), anyInt())).thenReturn(
            validDiscreteValidationResult()
        );

        validator.validateDeclarationOfIntent(file, SigningRule.SINGLE_SIGNATURE);

        assertThat(file.getBytes()).isEqualTo(originalBytes);
    }

    private DeclarationOfIntentValidator createValidator(
        TrustOnboardingSubmissionDoiValidationProperties.TrustOnboardingSubmissionDoiValidationMandantProperties mandant
    ) {
        return createValidator(discreteValidatorClient, mandant);
    }

    private static DeclarationOfIntentValidator createValidator(
        DiscreteValidatorClient client,
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
            client
        );
    }

    private static DiscreteValidationResult emptyDiscreteValidationResult() {
        return new DiscreteValidationResult(false, null, 0);
    }

    private static DiscreteValidationResult invalidDiscreteValidationResult() {
        return new DiscreteValidationResult(false, JsonNodeFactory.instance.objectNode(), 1);
    }

    private static DiscreteValidationResult validDiscreteValidationResult() {
        return new DiscreteValidationResult(true, JsonNodeFactory.instance.objectNode(), 1);
    }
}
