package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrustOnboardingSubmissionValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void invalidContactPhone_isReportedOnDomainValidation() {
        var submission = buildSubmission(
            new Contact("John", "Doe", "john@example.com", "0791234567", null),
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john@example.com"))
        );

        var violations = validator.validate(submission);

        assertThat(violations)
            .extracting(v -> v.getPropertyPath().toString())
            .anyMatch(path -> path.contains("contactPerson.phone"));
    }

    @Test
    void invalidSignatoryEmail_isReportedOnDomainValidation() {
        var submission = buildSubmission(
            new Contact("John", "Doe", "john@example.com", "+41 79 123 45 67", null),
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "not-an-email"))
        );

        var violations = validator.validate(submission);

        assertThat(violations)
            .extracting(v -> v.getPropertyPath().toString())
            .anyMatch(path -> path.contains("signatories") && path.endsWith("email"));
    }

    private static TrustOnboardingSubmission buildSubmission(Contact contact, List<Signatory> signatories) {
        return new TrustOnboardingSubmission(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Map.of("default", "Entity"),
            new Address(),
            "valid@example.com",
            contact,
            Language.DE,
            null,
            false,
            List.of(new ProofOfPossession("did:example:123", UUID.randomUUID().toString())),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            signatories,
            Instant.now()
        );
    }
}
