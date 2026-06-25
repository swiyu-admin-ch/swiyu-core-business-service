package ch.admin.bj.swiyu.core.business.modules.trust.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.limits.trust-onboarding-submission.doi-validation")
public record TrustOnboardingSubmissionDoiValidationProperties(
    TrustOnboardingSubmissionDoiValidationMandantProperties mandant,
    TrustOnboardingSubmissionDoiValidationDiscreteValidatorServiceProperties discreteValidatorService
) {
    @Validated
    @Getter
    public enum TrustOnboardingSubmissionDoiValidationMandantProperties {
        // Predefined so called "mandant" meaning validation profiles from the dieskreter validator api according
        // to chapter 8 of "Diskreter Validator Schnittstellenbeschreibung (auf Englisch)"
        // https://www.bit.admin.ch/de/diskreter-validator-web-service

        // This validator checks whether a document is validly signed with a
        //qualified certificate and a time stamp from a recognized provider
        //in accordance with ZertES. The presence of a valid time stamp
        //proving the exact time of signature is not necessary for a positive
        //validation for documents signed before 1.1.2017 (entry into force
        //of the revised Federal Act on Electronic Signature ZertES). All
        //signatures contained in the document must comply with these
        //criteria.
        QUALIFIED("Qualified"),

        // This validator checks whether a document has been signed with
        //an advanced certificate on a Swiss Government PKI smartcard
        //(FES) and provided with a time stamp from a recognized provider
        //in accordance with ZertES. All signatures contained in the
        //document must be valid and meet these criteria.
        SWISS_PKI("SwissGov-PKI"),

        // This validator is a technical client which is used if the document to
        //be validated has different types of electronic signatures, e.g. a
        //qualified signature (QES) and one or more advanced signatures
        //(FES), or a QES and a regulated seal, etc. The validator can also
        //be used as a validation client. Specific validation rules and reports
        //are defined for this technical client.
        MIXED("Mixed"),

        // No validation at all is performed / document is not sent to the remote validator
        NONE("");

        private final String value;

        TrustOnboardingSubmissionDoiValidationMandantProperties(String value) {
            this.value = value;
        }
    }

    @Validated
    public record TrustOnboardingSubmissionDoiValidationDiscreteValidatorServiceProperties(
        String url,
        String username,
        String password
    ) {}
}
