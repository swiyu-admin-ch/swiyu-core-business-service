package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import java.util.List;
import lombok.Getter;

@Getter
public enum SigningRule {
    SINGLE_SIGNATURE(1),
    JOINT_SIGNATURE_TWO(2),
    JOINT_SIGNATURE_THREE(3);

    private final int requiredSignatories;

    SigningRule(int requiredSignatories) {
        this.requiredSignatories = requiredSignatories;
    }

    // Returns how many signatories are required for the given business partner type.
    // Individual business partners don't need a signatory.
    public int getRequiredSignatories(BusinessPartnerType businessPartnerType) {
        if (
            List.of(BusinessPartnerType.GOVERNMENTAL_INSTITUTION, BusinessPartnerType.BUSINESS).contains(
                businessPartnerType
            )
        ) {
            return this.requiredSignatories;
        }

        return 0;
    }
}
