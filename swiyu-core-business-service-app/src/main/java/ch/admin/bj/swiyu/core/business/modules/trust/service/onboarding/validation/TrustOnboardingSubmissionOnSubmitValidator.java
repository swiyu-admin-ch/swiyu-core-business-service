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

    private static class Field {

        private static final String ENTITY_NAME = "entityName";
        private static final String ENTITY_EMAIL = "entityEmail";
        private static final String ENTITY_PROOFS = "proofOfPossessions";
        private static final String ENTITY_ADDRESS = "entityAddress";
        private static final String CONTACT_PERSON = "contactPerson";
        public static final String SIGNATORIES = "signatories";
        public static final String SIGNING_RULE = "signingRule";
    }

    static {
        REQUIRED_ACCESSORS.put(Field.ENTITY_NAME, TrustOnboardingSubmission::getEntityName); // Map
        REQUIRED_ACCESSORS.put(Field.ENTITY_ADDRESS, TrustOnboardingSubmission::getEntityAddress); // Object
        REQUIRED_ACCESSORS.put(Field.ENTITY_EMAIL, TrustOnboardingSubmission::getEntityEmail); // String
        REQUIRED_ACCESSORS.put(Field.CONTACT_PERSON, TrustOnboardingSubmission::getContactPerson); // Object
        REQUIRED_ACCESSORS.put(Field.ENTITY_PROOFS, TrustOnboardingSubmission::getProofOfPossessions);
    }

    private static class ErrorCode {

        public static final String INVALID = "INVALID";
        public static final String REQUIRED = "REQUIRED";
        public static final String BLANK = "BLANK";
    }

    private final TrustOnboardingSubmissionValidator trustOnboardingSubmissionValidator;

    public Errors validate(TrustOnboardingSubmission s, BusinessPartnerTypeDto businessPartnerType) {
        Errors errors = new SimpleErrors(s);

        trustOnboardingSubmissionValidator.validateTrustOnboardingSubmissionCanBeEdited(s, errors);

        // Generic required checks
        validateRequiredFields(s, errors);
        validateEntityNameDefaultTranslation(s, errors);

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
        if (!errors.hasFieldErrors(Field.ENTITY_EMAIL)) {
            String email = s.getEntityEmail();
            if (email != null && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
                errors.rejectValue(Field.ENTITY_EMAIL, "INVALID_FORMAT");
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
            errors.rejectValue("requestedPartnerType", ErrorCode.INVALID);
        }
    }

    private static void validateRequiredFields(TrustOnboardingSubmission s, Errors errors) {
        for (var entry : REQUIRED_ACCESSORS.entrySet()) {
            String path = entry.getKey();
            Object value = entry.getValue().apply(s);

            if (value == null) {
                errors.rejectValue(path, ErrorCode.REQUIRED);
                continue;
            }
            if (value instanceof String str && str.trim().isEmpty()) {
                errors.rejectValue(path, ErrorCode.BLANK);
            }
        }
    }

    private void validateEntityNameDefaultTranslation(TrustOnboardingSubmission s, Errors errors) {
        var nameErrors = validator.validateProperty(s, Field.ENTITY_NAME);
        if (nameErrors.isEmpty()) {
            return;
        }

        nameErrors.forEach(err -> errors.rejectValue(Field.ENTITY_NAME, ErrorCode.INVALID, err.getMessage()));
    }

    private static void validateProofs(TrustOnboardingSubmission s, Errors errors) {
        if (!errors.hasFieldErrors(Field.ENTITY_PROOFS)) {
            if (s.getProofOfPossessions().isEmpty()) {
                errors.rejectValue(Field.ENTITY_PROOFS, ErrorCode.INVALID);
            }
            for (var entry : s.getProofOfPossessions()) {
                if (entry.getStatus() != ProofOfPossessionStatus.VALID) {
                    errors.rejectValue(Field.ENTITY_PROOFS, ErrorCode.INVALID);
                }
            }
        }
    }

    private static void validateSigningRule(TrustOnboardingSubmission s, Errors errors) {
        if (s.getRequestedPartnerType() == BusinessPartnerType.INDIVIDUAL) {
            if (s.getSigningRule() != null) {
                errors.rejectValue(
                    Field.SIGNING_RULE,
                    ErrorCode.INVALID,
                    "Signing rule must be null for INDIVIDUAL business partner type"
                );
            }
            if (s.getSignatories() != null && !s.getSignatories().isEmpty()) {
                errors.rejectValue(
                    Field.SIGNATORIES,
                    ErrorCode.INVALID,
                    "Signatories must be empty for INDIVIDUAL business partner type"
                );
            }
            return;
        }

        if (s.getSigningRule() == null) {
            errors.rejectValue(Field.SIGNING_RULE, ErrorCode.REQUIRED);
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
                    errors.rejectValue(Field.SIGNATORIES, ErrorCode.INVALID, violation.getMessage())
                );
            }
        }
    }

    private void validateContactPerson(TrustOnboardingSubmission s, Errors errors) {
        if (s.getContactPerson() != null) {
            var violations = validator.validate(s.getContactPerson());
            violations.forEach(violation ->
                errors.rejectValue(Field.CONTACT_PERSON, ErrorCode.INVALID, violation.getMessage())
            );
        }
    }

    private static void validateDeclarationOfIntentPresent(TrustOnboardingSubmission s, Errors errors) {
        if (s.getDeclarationOfIntent() == null) {
            errors.rejectValue(
                "declarationOfIntent",
                ErrorCode.REQUIRED,
                "Declaration of intent must be uploaded before submitting."
            );
        }
    }
}
