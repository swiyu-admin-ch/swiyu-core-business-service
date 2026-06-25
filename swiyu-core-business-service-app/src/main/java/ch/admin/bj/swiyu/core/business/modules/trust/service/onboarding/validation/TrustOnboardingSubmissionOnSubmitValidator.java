package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import static ch.admin.bj.swiyu.core.business.common.validation.EmailValidation.EMAIL_REGEX;
import static ch.admin.bj.swiyu.core.business.modules.trust.api.SigningRuleValidatorErrorcodeDto.SIGNING_RULE_SIGNATORY_COUNT_MISMATCH;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossessionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.SimpleErrors;

@Component
@RequiredArgsConstructor
public class TrustOnboardingSubmissionOnSubmitValidator {

    private final FeaturesProperties featuresProperties;
    private final Validator validator;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    /**
     * Declarative list of "required" fields. The key is the logical field path you want
     * in error messages; the value is a getter that extracts the value.
     * <p>
     * For String values: null → REQUIRED, blank → BLANK
     * For non-String values: null → REQUIRED
     */
    private static final Map<String, Function<TrustOnboardingSubmission, Object>> REQUIRED_ACCESSORS =
        new LinkedHashMap<>();
    // sonar complains about duplicate usage of string literals
    private static final String ENTITY_EMAIL = "entityEmail";
    private static final String ENTITY_PROOFS = "proofOfPossessions";

    static {
        REQUIRED_ACCESSORS.put("entityName", TrustOnboardingSubmission::getEntityName); // String
        REQUIRED_ACCESSORS.put("entityAddress", TrustOnboardingSubmission::getEntityAddress); // Object
        REQUIRED_ACCESSORS.put(ENTITY_EMAIL, TrustOnboardingSubmission::getEntityEmail); // String
        REQUIRED_ACCESSORS.put("contactPerson", TrustOnboardingSubmission::getContactPerson); // Object
        REQUIRED_ACCESSORS.put(ENTITY_PROOFS, TrustOnboardingSubmission::getProofOfPossessions);
    }

    private static final String INVALID_ERROR_CODE = "INVALID";
    private static final String REQUIRED_ERROR_CODE = "REQUIRED";

    private final TrustOnboardingSubmissionValidator trustOnboardingSubmissionValidator;

    public Errors validate(TrustOnboardingSubmission s, BusinessPartnerTypeDto businessPartnerType) {
        Errors errors = new SimpleErrors(s);

        trustOnboardingSubmissionValidator.validateTrustOnboardingSubmissionCanBeEdited(s, errors);

        // Generic required checks
        validateRequiredFields(s, errors);

        validateSigningRule(s, errors);

        validateSignatories(s, errors);

        validateContactPerson(s, errors);

        if (Boolean.TRUE.equals(featuresProperties.getEidartfe1220ProofOfPossession())) {
            // POP check
            validateProofs(s, errors);
        }

        // Email format check (only if present and not blank)
        validateEmailFormat(s, errors);

        // requested partner type check
        validatePartnerTypeGovernmentDidNotChange(s.getRequestedPartnerType(), businessPartnerType, errors);

        validateDeclarationOfIntentPresent(s, errors);

        return errors;
    }

    private static void validateEmailFormat(TrustOnboardingSubmission s, Errors errors) {
        if (!errors.hasFieldErrors(ENTITY_EMAIL)) {
            String email = s.getEntityEmail();
            if (email != null && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
                errors.rejectValue(ENTITY_EMAIL, "INVALID_FORMAT");
            }
        }
    }

    private static void validatePartnerTypeGovernmentDidNotChange(
        BusinessPartnerType requestedBusinessPartnerType,
        BusinessPartnerTypeDto partnerType,
        Errors errors
    ) {
        if (
            requestedBusinessPartnerType == BusinessPartnerType.GOVERNMENTAL_INSTITUTION &&
            partnerType != BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION
        ) {
            errors.rejectValue("requestedPartnerType", INVALID_ERROR_CODE);
        }
    }

    private static void validateRequiredFields(TrustOnboardingSubmission s, Errors errors) {
        for (var entry : REQUIRED_ACCESSORS.entrySet()) {
            String path = entry.getKey();
            Object value = entry.getValue().apply(s);

            if (value == null) {
                errors.rejectValue(path, REQUIRED_ERROR_CODE);
                continue;
            }
            if (value instanceof String str && str.trim().isEmpty()) {
                errors.rejectValue(path, "BLANK");
            }
        }
    }

    private static void validateProofs(TrustOnboardingSubmission s, Errors errors) {
        if (!errors.hasFieldErrors(ENTITY_PROOFS)) {
            for (var entry : s.getProofOfPossessions()) {
                if (entry.getStatus() != ProofOfPossessionStatus.VALID) {
                    errors.rejectValue(ENTITY_PROOFS, INVALID_ERROR_CODE);
                }
            }
        }
    }

    private static void validateSigningRule(TrustOnboardingSubmission s, Errors errors) {
        if (s.getRequestedPartnerType() == BusinessPartnerType.INDIVIDUAL) {
            if (s.getSigningRule() != null) {
                errors.rejectValue(
                    "signingRule",
                    INVALID_ERROR_CODE,
                    "Signing rule must be null for INDIVIDUAL business partner type"
                );
            }
            if (s.getSignatories() != null && !s.getSignatories().isEmpty()) {
                errors.rejectValue(
                    "signatories",
                    INVALID_ERROR_CODE,
                    "Signatories must be empty for INDIVIDUAL business partner type"
                );
            }
            return;
        }

        if (s.getSigningRule() == null) {
            errors.rejectValue("signingRule", REQUIRED_ERROR_CODE);
            return;
        }

        var requiredSignatories = s.getSigningRule().getRequiredSignatories(s.getRequestedPartnerType());
        var signatoriesCount = s.getSignatories() == null ? 0 : s.getSignatories().size();
        if (requiredSignatories != signatoriesCount) {
            errors.reject(
                SIGNING_RULE_SIGNATORY_COUNT_MISMATCH.toString(),
                "Signing rule '%s' with business partner type '%s' requires %d signatories, but %d were provided".formatted(
                    s.getSigningRule(),
                    s.getRequestedPartnerType(),
                    requiredSignatories,
                    signatoriesCount
                )
            );
        }
    }

    private void validateSignatories(TrustOnboardingSubmission s, Errors errors) {
        if (s.getSignatories() != null) {
            for (var signatory : s.getSignatories()) {
                var violations = validator.validate(signatory);
                violations.forEach(violation ->
                    errors.rejectValue("signatories", INVALID_ERROR_CODE, violation.getMessage())
                );
            }
        }
    }

    private void validateContactPerson(TrustOnboardingSubmission s, Errors errors) {
        if (s.getContactPerson() != null) {
            var violations = validator.validate(s.getContactPerson());
            violations.forEach(violation ->
                errors.rejectValue("contactPerson", INVALID_ERROR_CODE, violation.getMessage())
            );
        }
    }

    private static void validateDeclarationOfIntentPresent(TrustOnboardingSubmission s, Errors errors) {
        if (s.getDeclarationOfIntent() == null) {
            errors.rejectValue(
                "declarationOfIntent",
                REQUIRED_ERROR_CODE,
                "Declaration of intent must be uploaded before submitting."
            );
        }
    }
}
