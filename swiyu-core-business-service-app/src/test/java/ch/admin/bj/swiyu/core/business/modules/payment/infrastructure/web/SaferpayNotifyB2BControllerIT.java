package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.insertTestBusinessPartners;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
class SaferpayNotifyB2BControllerIT {

    private static final String NOTIFY_URL = "/api/v1/saferpay/notify";
    private static final String READ_ROLE = "ti_@payment_#read";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestRepositories repos;

    @MockitoBean
    TransactionalOutbox transactionalOutbox;

    @BeforeEach
    void setUp() {
        repos.truncateTables();
        insertTestBusinessPartners(repos.businessPartner);
        repos.payment.deleteAll();
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void notify_existingPayment_publishesEventAndReturns200() throws Exception {
        var payment = createTestPayment("P-ABC12345");
        repos.payment.saveAndFlush(payment);

        mockMvc.perform(get(NOTIFY_URL).param("referenceId", "P-ABC12345")).andExpect(status().isOk());

        verify(transactionalOutbox).sendMessage(any(), eq(payment.getId().toString()), anyString());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void notify_nonExistingPayment_returns200() throws Exception {
        mockMvc.perform(get(NOTIFY_URL).param("referenceId", "P-NOTEXIST")).andExpect(status().isOk());

        verify(transactionalOutbox, never()).sendMessage(any(), any(), anyString());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void notify_integration_withDatabase_publishesEvent() throws Exception {
        var payment = new Payment(
            UUID.randomUUID(),
            ENTITY_A,
            "P-INTEGRATION123",
            ProductType.BASE_REGISTRATION,
            Amount.chf(100),
            "https://portal.example.com/callback/123"
        );
        repos.payment.saveAndFlush(payment);

        mockMvc.perform(get(NOTIFY_URL).param("referenceId", "P-INTEGRATION123")).andExpect(status().isOk());

        verify(transactionalOutbox).sendMessage(any(), eq(payment.getId().toString()), anyString());
    }

    private Payment createTestPayment(String referenceId) {
        return new Payment(
            UUID.randomUUID(),
            ENTITY_A,
            referenceId,
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
    }
}
