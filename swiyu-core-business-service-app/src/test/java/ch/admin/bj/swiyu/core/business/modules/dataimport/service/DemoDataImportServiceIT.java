package ch.admin.bj.swiyu.core.business.modules.dataimport.service;

import static ch.admin.bj.swiyu.core.business.common.security.SystemUserAuthenticationSupport.setSystemSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.DemoData;
import ch.admin.bj.swiyu.core.business.modules.dataimport.domain.DemoPaymentData;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles({ "test", "test-data-injection" })
@SpringBootTest
@WithAllTestContainerInitializers
class DemoDataImportServiceIT {

    @Autowired
    DemoDataImportService demoDataImportService;

    @Autowired
    TestRepositories repos;

    @MockitoBean
    ScanApi scanApi;

    @MockitoBean
    ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher auditPublisher;

    @MockitoBean
    ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader didPublicKeyLoader;

    @MockitoBean
    DemoDataAsyncExecutor demoDataAsyncExecutor;

    @MockitoBean
    PamsClient pamsClient;

    @BeforeEach
    void setUp() {
        repos.truncateTables();

        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .result("OK")
                    .requestID(UUID.randomUUID())
                    .description("description")
                    .clamavVersion("clamav-v1")
                    .clamavDatabaseVersion("clamav-db-v1")
            )
        );
    }

    @Test
    void generatedPayments_canBeReimported() {
        setSystemSecurityContext();
        demoDataImportService.generatePayments();
        demoDataImportService.deleteDemoPayments();
        demoDataImportService.generatePayments();

        assertThat(repos.payment.findAll()).hasSize(DemoPaymentData.PAYMENTS.size());
        assertThat(repos.payment.findAll())
            .extracting(Payment::getStatus)
            .contains(PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.SUCCEEDED);
    }

    @Test
    void generatedPayments_pendingPaymentOfTester_isKeptAndDemoPendingPaymentSkipped() {
        setSystemSecurityContext();
        var demoPartnerId = DemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp.id();
        var testerPayment = repos.payment.saveAndFlush(
            new Payment(
                UUID.randomUUID(),
                demoPartnerId,
                "P-0000FFFF",
                ProductType.BASE_REGISTRATION,
                Amount.chf(15_000),
                "https://portal.example.local/callback"
            )
        );

        demoDataImportService.deleteDemoPayments();
        demoDataImportService.generatePayments();

        var demoPendingPaymentId = DemoPaymentData.PAYMENTS.stream()
            .filter(payment -> payment.businessPartnerId().equals(demoPartnerId))
            .findFirst()
            .orElseThrow()
            .id();
        assertThat(repos.payment.findById(testerPayment.getId())).isPresent();
        assertThat(repos.payment.findById(demoPendingPaymentId)).isEmpty();
        assertThat(repos.payment.findAll()).hasSize(DemoPaymentData.PAYMENTS.size());
    }

    @Test
    void deleteDemoPayments_paymentOfTesterForDemoPartner_isKept() {
        setSystemSecurityContext();
        var demoPartnerId = DemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp.id();
        var testerPayment = new Payment(
            UUID.randomUUID(),
            demoPartnerId,
            "P-0000FFFE",
            ProductType.BASE_REGISTRATION,
            Amount.chf(15_000),
            "https://portal.example.local/callback"
        );
        testerPayment.markAsFailed();
        repos.payment.saveAndFlush(testerPayment);
        demoDataImportService.generatePayments();

        demoDataImportService.deleteDemoPayments();

        assertThat(repos.payment.findAll()).extracting(Payment::getId).containsExactly(testerPayment.getId());
    }

    @Test
    void generatedSubmissionsPersistTheirType() {
        setSystemSecurityContext();
        demoDataImportService.generateBusinessPartners();
        demoDataImportService.generateTrustOnboardingSubmissions();

        var renewal = repos.trustOnboardingSubmission.findById(UUID.fromString("4b1a77c2-9d3e-4f2a-8c5d-1e0b6f7a8c9d"));
        assertThat(renewal).isPresent();
        assertThat(renewal.orElseThrow().getType()).isEqualTo(TrustOnboardingSubmissionType.RENEWAL);

        var profileChange = repos.trustOnboardingSubmission.findById(
            UUID.fromString("3299cd25-8bab-47b7-9d46-f740be76e57e")
        );
        assertThat(profileChange).isPresent();
        assertThat(profileChange.orElseThrow().getType()).isEqualTo(
            TrustOnboardingSubmissionType.PROFILE_CHANGE_MANDATORY
        );
    }

    @Test
    void reimportingDemoData_overwritesThePartnersAndReappliesTheIdentities() {
        setSystemSecurityContext();
        demoDataImportService.generateBusinessPartners();
        demoDataImportService.generateBusinessPartnerIdentities();
        var identityCount = repos.businessPartnerIdentity.count();
        assertThat(identityCount).isPositive();

        // re-import over the existing partners and identities
        demoDataImportService.generateBusinessPartners();
        demoDataImportService.generateBusinessPartnerIdentities();

        assertThat(repos.businessPartnerIdentity.count()).isEqualTo(identityCount);
    }
}
