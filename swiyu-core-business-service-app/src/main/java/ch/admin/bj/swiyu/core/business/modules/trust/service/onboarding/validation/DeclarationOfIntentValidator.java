package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import static ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto.*;
import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule.*;

import ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidationResult;
import ch.admin.bj.swiyu.core.business.modules.trust.api.DeclarationOfIntentValidatorErrorCodeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionDoiValidationProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DeclarationOfIntentValidationException;
import ch.admin.bj.swiyu.discrete.validator.DiscreteValidationResult;
import ch.admin.bj.swiyu.discrete.validator.DiscreteValidatorClient;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@AllArgsConstructor
@Slf4j
public class DeclarationOfIntentValidator {

    private final TrustOnboardingSubmissionDoiValidationProperties validationProperties;
    private final DiscreteValidatorClient discreteValidatorClient;

    public DeclarationOfIntentValidationResult validateDeclarationOfIntent(
        MultipartFile file,
        SigningRule signingRule
    ) {
        try {
            var result = discreteValidatorClient.executeValidationRequest(
                file,
                validationProperties.mandant().getValue(),
                signingRule.getRequiredSignatories()
            );
            validateValidationResponse(signingRule, result);
            return new DeclarationOfIntentValidationResult(result.fileReport());
        } catch (DeclarationOfIntentValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new DeclarationOfIntentValidationException(List.of(VALIDATION_SERVICE_NOT_AVAILABLE.toString()), e);
        }
    }

    private void validateValidationResponse(
        SigningRule signingRule,
        DiscreteValidationResult signatureValidationResult
    ) {
        List<DeclarationOfIntentValidatorErrorCodeDto> violations = new ArrayList<>();
        if (!signatureValidationResult.isValidStatus()) {
            violations.add(DeclarationOfIntentValidatorErrorCodeDto.INVALID_SIGNATURES_FOR_MANDANT);
        }
        if (signatureValidationResult.fileReport() == null) {
            violations.add(NO_SIGNATURES_FOUND);
        }
        var amountOfSignatures = signatureValidationResult.amountOfSignatures();
        if (amountOfSignatures == 0) {
            violations.add(NO_SIGNATURES_FOUND);
        }

        if (signingRule == SINGLE_SIGNATURE && amountOfSignatures != SINGLE_SIGNATURE.getRequiredSignatories()) {
            violations.add(VIOLATING_DOI_VARIANT_SINGLE_SIGNATURE);
        } else if (
            signingRule == JOINT_SIGNATURE_TWO && amountOfSignatures != JOINT_SIGNATURE_TWO.getRequiredSignatories()
        ) {
            violations.add(VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_TWO);
        } else if (
            signingRule == JOINT_SIGNATURE_THREE && amountOfSignatures != JOINT_SIGNATURE_THREE.getRequiredSignatories()
        ) {
            violations.add(VIOLATING_DOI_VARIANT_JOINT_SIGNATURE_THREE);
        }
        if (!violations.isEmpty()) {
            throw new DeclarationOfIntentValidationException(
                violations.stream().map(DeclarationOfIntentValidatorErrorCodeDto::toString).toList()
            );
        }
    }
}
