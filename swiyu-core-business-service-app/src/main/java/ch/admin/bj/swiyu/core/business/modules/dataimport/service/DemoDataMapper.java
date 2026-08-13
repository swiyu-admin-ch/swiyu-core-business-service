package ch.admin.bj.swiyu.core.business.modules.dataimport.service;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.DemoData;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.Signatory;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DemoDataMapper {

    public static BusinessEntity toBusinessEntity(DemoData.DemoBusinessPartner bp) {
        return new BusinessEntity(
            bp.id(),
            LocalizedMapUtil.getDefaultValue(bp.names()),
            bp.email(),
            toBusinessPartnerType(bp.type()),
            toAddress(bp.address()),
            bp.uid(),
            bp.contactPhone()
        );
    }

    static BusinessPartnerType toBusinessPartnerType(DemoData.DemoBusinessPartner.DemoBusinessPartnerType type) {
        return switch (type) {
            case GOVERNMENTAL_INSTITUTION -> BusinessPartnerType.GOVERNMENTAL_INSTITUTION;
            case BUSINESS -> BusinessPartnerType.BUSINESS;
            case INDIVIDUAL -> BusinessPartnerType.INDIVIDUAL;
        };
    }

    static Address toAddress(DemoData.DemoBusinessPartner.DemoAddress address) {
        return address == null
            ? null
            : new Address(address.street(), address.city(), address.postalCode(), address.country(), address.region());
    }

    static Contact toContact(DemoData.DemoBusinessPartner.DemoContact contact) {
        return contact == null
            ? null
            : new Contact(
                  contact.firstName(),
                  contact.lastName(),
                  contact.email(),
                  contact.phone(),
                  toLanguage(contact.correspondingLanguage())
              );
    }

    static Language toLanguage(DemoData.DemoBusinessPartner.DemoContact.Language language) {
        return switch (language) {
            case DE -> Language.DE;
            case EN -> Language.EN;
            case FR -> Language.FR;
            case IT -> Language.IT;
            case RM -> Language.RM;
            case null -> null;
        };
    }

    public static SigningRule toSignatoryRule(DemoData.DemoBusinessPartner.DemoSigningRule signatoryRule) {
        return switch (signatoryRule) {
            case SINGLE_SIGNATURE -> SigningRule.SINGLE_SIGNATURE;
            case JOINT_SIGNATURE_TWO -> SigningRule.JOINT_SIGNATURE_TWO;
            case JOINT_SIGNATURE_THREE -> SigningRule.JOINT_SIGNATURE_THREE;
            case null -> null;
        };
    }

    @SuppressWarnings(
        {
            "java:S1168", // Allow null as it does have a different meaning than the enforced empty collection on DB level
        }
    )
    public static List<Signatory> toSignatoryList(List<DemoData.DemoBusinessPartner.DemoSignatory> signatory) {
        if (signatory == null) {
            return null;
        }
        return signatory
            .stream()
            .map(sig -> new Signatory(sig.firstName(), sig.lastName(), sig.phone(), sig.email()))
            .toList();
    }

    public static @Valid @NotNull TrustOnboardingSubmissionDocumentTypeDto toTrustOnboardingSubmissionDocumentTypeDto(
        DemoData.DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto type
    ) {
        return switch (type) {
            case TRUST_ONBOARDING_OTHER -> TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER;
            case TRUST_ONBOARDING_DECLARATION_OF_INTENT -> TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT;
        };
    }

    public static BusinessPartnerIdentityStatus toBusinessPartnerIdentityStatus(
        DemoData.DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoBusinessPartnerIdentityStatus status
    ) {
        return switch (status) {
            case DEACTIVATED -> BusinessPartnerIdentityStatus.DEACTIVATED;
            case ACTIVE -> BusinessPartnerIdentityStatus.ACTIVE;
        };
    }
}
