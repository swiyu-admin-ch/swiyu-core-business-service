package ch.admin.bj.swiyu.core.business.modules.email.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import java.time.Duration;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the one branch the job's integration test cannot reach cheaply: the functionality flag.
 *
 * <p>Everything else about this service - which candidates it finds, the window, the paging - is
 * tested against real data in {@code EmailNotificationJobIT}. Only the "do nothing at all" case needs
 * a second {@link FunctionalityProperties}, and spinning up a second Spring context for one boolean
 * would cost a minute of build time.
 *
 * <p>The two query services are mocked here as collaborators, not as data: the point of the test is
 * that they are never called.
 */
class EmailNotificationServiceTest {

    private BusinessPartnerService businessPartnerService;
    private TrustOnboardingService trustOnboardingService;
    private EmailCommandPublisher emailCommandPublisher;

    @BeforeEach
    void setUp() {
        businessPartnerService = mock(BusinessPartnerService.class);
        trustOnboardingService = mock(TrustOnboardingService.class);
        emailCommandPublisher = mock(EmailCommandPublisher.class);
        // The lock is held by @SchedulerLock in production; there is no proxy around a hand-built service
        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @AfterEach
    void tearDown() {
        LockAssert.TestHelper.makeAllAssertsPass(false);
    }

    /**
     * All stages run with the functionality off for now. The job must then not even look for
     * candidates - otherwise it logs a candidate count every night that reads as if reminders were
     * going out.
     */
    @Test
    void looksForNothingWhileTheFunctionalityIsDisabled() {
        serviceWithEmailFunctionality(false).publishDueNotifications(Duration.ofDays(4), 100);

        verifyNoInteractions(businessPartnerService, trustOnboardingService, emailCommandPublisher);
    }

    private EmailNotificationService serviceWithEmailFunctionality(boolean emailEnabled) {
        return new EmailNotificationService(
            businessPartnerService,
            trustOnboardingService,
            emailCommandPublisher,
            new FunctionalityProperties(emailEnabled)
        );
    }
}
