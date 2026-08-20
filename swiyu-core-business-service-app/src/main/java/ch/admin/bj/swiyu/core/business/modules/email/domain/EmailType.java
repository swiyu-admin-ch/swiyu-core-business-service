package ch.admin.bj.swiyu.core.business.modules.email.domain;

import lombok.Getter;

/**
 * Type of a partner notification email.
 *
 * <p>The enum value name is what ends up in {@code TiSendEmailCommandPayload.emailType} and is
 * therefore part of the contract towards the consumer of the command.
 */
@Getter
public enum EmailType {
    SUBMISSION_ACCEPTED("submission-accepted"),
    TRUST_REGISTRATION_SUCCEEDED("trust-registration-succeeded"),
    TRUST_REGISTRATION_REJECTED("trust-registration-rejected"),
    TRUST_REGISTRATION_INFORMATION_REQUESTED("trust-registration-information-requested"),
    TRUST_PROFILE_CHANGE_SUCCEEDED("trust-profile-change-succeeded"),
    TRUST_PROFILE_CHANGE_REJECTED("trust-profile-change-rejected"),
    TRUST_PROFILE_CHANGE_INFORMATION_REQUESTED("trust-profile-change-information-requested"),
    TRUST_RENEWAL_SUCCEEDED("trust-renewal-succeeded"),
    TRUST_IDENTITY_EXPIRED("trust-identity-expired"),
    TRUST_ADDITIONAL_DID_ADDED("trust-additional-did-added"),
    TRUST_PROTECTED_VERIFICATION_AHV_APPROVED("trust-protected-verification-ahv-approved"),
    TRUST_PROTECTED_VERIFICATION_AHV_REJECTED("trust-protected-verification-ahv-rejected");

    /**
     * Name of the Thymeleaf template under {@code src/main/resources/email-templates}, without the
     * {@code .txt} suffix.
     */
    private final String templateName;

    EmailType(String templateName) {
        this.templateName = templateName;
    }
}
