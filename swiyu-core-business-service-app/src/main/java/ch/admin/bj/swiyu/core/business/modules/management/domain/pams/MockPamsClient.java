package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * Mocked pams client, which can be used locally or on stages without PAMS.
 */
@Slf4j
public class MockPamsClient implements PamsClient {

    public MockPamsClient() {
        log.info("Running with mocked pams client");
    }

    @Override
    public void createBusinessPartner(BusinessEntity businessPartner, String pamsUserAdminDirUid) {
        log.debug("mocking invocation of createBusinessPartner");
    }

    @Override
    public void deleteBusinessPartner(String foreignId) {
        log.debug("mocking invocation of deleteBusinessPartner");
    }

    @Override
    public void updateBusinessPartner(BusinessEntity businessPartner) {
        log.debug("mocking invocation of updateBusinessPartner");
    }

    @Override
    public void getHealth() {
        log.debug("mocking invocation of getHealth");
    }
}
