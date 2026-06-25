package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import static ch.admin.bj.swiyu.core.business.modules.trust.api.SigningRuleValidatorErrorcodeDto.SIGNING_RULE_SIGNATORY_COUNT_MISMATCH;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmissionEmpty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.domain.MultiLanguageText;
import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.DeclarationOfIntent;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.Signatory;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.FieldError;

@ExtendWith(MockitoExtension.class)
class TrustOnboardingSubmissionOnSubmitValidatorTest {

    @Mock
    private FeaturesProperties featuresProperties;

    @Mock
    private TrustOnboardingSubmissionValidator trustOnboardingSubmissionValidator;

    private Validator beanValidator;

    private TrustOnboardingSubmissionOnSubmitValidator validator;

    @BeforeEach
    void setUp() {
        beanValidator = Validation.buildDefaultValidatorFactory().getValidator();
        validator = new TrustOnboardingSubmissionOnSubmitValidator(
            featuresProperties,
            beanValidator,
            trustOnboardingSubmissionValidator
        );
        when(featuresProperties.getEidartfe1220ProofOfPossession()).thenReturn(true);
    }

    @Test
    void succeeds_when_all_required_fields_present() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            List.of(new ProofOfPossession("did", UUID.randomUUID().toString()).toValid()),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );
        s.updateDeclarationOfIntent(new DeclarationOfIntent(UUID.randomUUID().toString(), null));
        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertFalse(violations.hasErrors());
    }

    @Test
    void succeeds_when_all_required_fields_present_gov() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            null, // Gov actors require no UID
            List.of(new ProofOfPossession("did", UUID.randomUUID().toString()).toValid()),
            BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );
        s.updateDeclarationOfIntent(new DeclarationOfIntent(UUID.randomUUID().toString(), null));
        var violations = validator.validate(s, BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION);
        assertFalse(violations.hasErrors());
    }

    @Test
    void fails_when_requested_partner_type_gov_but_partner_not_gov() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            List.of(new ProofOfPossession("did", UUID.randomUUID().toString()).toValid()),
            BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );
        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(violations.hasErrors());
    }

    @Test
    void fails_when_missing_field() {
        var s = trustOnboardingSubmissionEmpty();
        // entityEmail missing
        s.update(
            new MultiLanguageText(),
            new Address(),
            null,
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "entityEmail".equals(((FieldError) v).getField()) && "REQUIRED".equals(v.getCode()))
        );
    }

    @Test
    void fails_when_email_invalid_format() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "not-an-email",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(
                    v -> "entityEmail".equals(((FieldError) v).getField()) && "INVALID_FORMAT".equals(v.getCode())
                )
        );
    }

    @Test
    void fails_when_proof_of_possession_is_not_valid() {
        var s = createValidSubmission();
        s.update(
            s.getEntityName(),
            s.getEntityAddress(),
            s.getEntityEmail(),
            s.getContactPerson(),
            s.getCorrespondingLanguage(),
            s.getUid(),
            List.of(new ProofOfPossession("", UUID.randomUUID().toString())),
            BusinessPartnerType.BUSINESS,
            s.getSigningRule(),
            s.getSignatories(),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);

        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(
                    v -> "proofOfPossessions".equals(((FieldError) v).getField()) && "INVALID".equals(v.getCode())
                )
        );
    }

    @Test
    void succeeds_when_proof_of_possession_is_valid() {
        var s = createValidSubmission();
        s.update(
            s.getEntityName(),
            s.getEntityAddress(),
            s.getEntityEmail(),
            s.getContactPerson(),
            s.getCorrespondingLanguage(),
            s.getUid(),
            List.of(new ProofOfPossession("", UUID.randomUUID().toString()).toValid()),
            BusinessPartnerType.BUSINESS,
            s.getSigningRule(),
            s.getSignatories(),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);

        assertFalse(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "proofOfPossessions".equals(((FieldError) v).getField()))
        );
    }

    @Test
    void succeeds_when_pop_feature_is_disabled_and_pop_is_invalid() {
        when(featuresProperties.getEidartfe1220ProofOfPossession()).thenReturn(false);

        var s = createValidSubmission();
        s.update(
            s.getEntityName(),
            s.getEntityAddress(),
            s.getEntityEmail(),
            s.getContactPerson(),
            s.getCorrespondingLanguage(),
            s.getUid(),
            List.of(new ProofOfPossession("", UUID.randomUUID().toString())),
            BusinessPartnerType.BUSINESS,
            s.getSigningRule(),
            s.getSignatories(),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);

        assertFalse(violations.hasErrors());
    }

    @Test
    void fails_when_signing_rule_missing() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            null,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "signingRule".equals(((FieldError) v).getField()) && "REQUIRED".equals(v.getCode()))
        );
    }

    @Test
    void fails_when_signing_rule_signatory_count_mismatch() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            SigningRule.JOINT_SIGNATURE_TWO,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> SIGNING_RULE_SIGNATORY_COUNT_MISMATCH.toString().equals(v.getCode()))
        );
    }

    @Test
    void fails_when_signing_rule_not_null_for_individual() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.INDIVIDUAL,
            SigningRule.SINGLE_SIGNATURE,
            null,
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.INDIVIDUAL);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "signingRule".equals(((FieldError) v).getField()) && "INVALID".equals(v.getCode()))
        );
    }

    @Test
    void fails_when_signatories_not_empty_for_individual() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.INDIVIDUAL,
            null,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.INDIVIDUAL);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "signatories".equals(((FieldError) v).getField()) && "INVALID".equals(v.getCode()))
        );
    }

    @Test
    void succeeds_when_signing_rule_and_signatories_null_for_individual() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            List.of(new ProofOfPossession("did", UUID.randomUUID().toString()).toValid()),
            BusinessPartnerType.INDIVIDUAL,
            null,
            List.of(),
            true
        );
        s.updateDeclarationOfIntent(new DeclarationOfIntent(UUID.randomUUID().toString(), null));

        var violations = validator.validate(s, BusinessPartnerTypeDto.INDIVIDUAL);
        assertFalse(violations.hasErrors());
    }

    @Test
    void fails_when_signatory_invalid() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "signatories".equals(((FieldError) v).getField()) && "INVALID".equals(v.getCode()))
        );
    }

    @Test
    void fails_when_signatory_phone_invalid_format() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "0791234567", "john@example.org")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "signatories".equals(((FieldError) v).getField()) && "INVALID".equals(v.getCode()))
        );
    }

    @Test
    void fails_when_signatory_email_invalid_format() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "not-an-email")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "signatories".equals(((FieldError) v).getField()) && "INVALID".equals(v.getCode()))
        );
    }

    private TrustOnboardingSubmission createValidSubmission() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            validContact(),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );
        s.updateDeclarationOfIntent(new DeclarationOfIntent(UUID.randomUUID().toString(), null));
        return s;
    }

    @Test
    void fails_when_contact_phone_invalid_format() {
        var s = trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText(),
            new Address(),
            "valid@example.org",
            new Contact("John", "Doe", "john@example.org", "0791234567", null),
            Language.DE,
            "uid",
            null,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.org")),
            true
        );

        var violations = validator.validate(s, BusinessPartnerTypeDto.BUSINESS);
        assertTrue(
            violations
                .getAllErrors()
                .stream()
                .anyMatch(v -> "contactPerson".equals(((FieldError) v).getField()) && "INVALID".equals(v.getCode()))
        );
    }

    private Contact validContact() {
        return new Contact("John", "Doe", "john@example.org", "+41 79 123 45 67", null);
    }
}
