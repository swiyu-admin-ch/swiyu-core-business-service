package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.insertTestBusinessPartners;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken
class PaymentRepositoryTest {

    @Autowired
    TestRepositories repos;

    @BeforeEach
    void setUp() {
        insertTestBusinessPartners(repos.businessPartner);
        repos.payment.deleteAll();
    }

    @Test
    void findByReferenceId_existing_returnsPayment() {
        var payment = createTestPayment("P-ABC12345");
        repos.payment.saveAndFlush(payment);

        var found = repos.payment.findByReferenceId("P-ABC12345");

        assertThat(found).isPresent();
        assertThat(found.get().getReferenceId()).isEqualTo("P-ABC12345");
    }

    @Test
    void findByReferenceId_notExisting_returnsEmpty() {
        var found = repos.payment.findByReferenceId("P-NOTEXIST");

        assertThat(found).isEmpty();
    }

    private Payment createTestPayment(String referenceId) {
        return new Payment(
            UUID.randomUUID(),
            ENTITY_A,
            referenceId,
            ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType.BASE_REGISTRATION,
            ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
    }
}
