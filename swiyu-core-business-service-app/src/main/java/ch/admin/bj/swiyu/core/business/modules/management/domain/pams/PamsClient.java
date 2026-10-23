package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;

public interface PamsClient {
    void createBusinessPartner(BusinessEntity businessPartner, String pamsUserAdminDirUid);

    void updateBusinessPartnerName(BusinessEntity businessPartner);

    void deleteBusinessPartner(String foreignId);

    void getHealth();
}
