package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.common.security.SystemUserAuthenticationSupport.setSystemSecurityContext;
import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus.SUBMITTED;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeBusiness;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static ch.admin.bj.swiyu.core.business.test.pact.PactProviderSupport.*;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.AllowOverridePactUrl;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionRepository;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT // port defined in application-test.yml
)
@WithAllTestContainerInitializers
@Import({ JeapOAuth2IntegrationTestResourceConfiguration.class })
@Provider("swiyu-core-business-service")
@PactBroker
@EmbeddedKafka
@AllowOverridePactUrl // Allow externally specified pact url to override other consumer version selectors.
// For providers that do not yet or no longer have a consumer. Remove annotation when at least one consumer pact is expected to be available.
@IgnoreNoPactsToVerify
@Slf4j
class TrustOnboardingSubmissionPactProviderTest {

    @LocalServerPort
    int port;

    @Autowired
    private TrustOnboardingSubmissionRepository submissionRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        setupPactHttpTarget(context, port);
    }

    @BeforeAll
    static void init() {
        // here set the system property "pactbroker.consumerversionselectors.rawjson" in case you want to fetch branch specific consumer tests
        // see https://confluence.bit.admin.ch/spaces/JEAP/pages/684758576/Details+zur+jEAP-Integration+von+Pact
    }

    @TestTemplate
    @ExtendWith(au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider.class)
    void testPacts(PactVerificationContext context) {
        if (context == null) {
            log.info("No pact file was specified. Skipping verification.");
            return; // If there is no pact, there will be no context
        }
        context.verifyInteraction();
    }

    @State(
        "A submitted TrustOnboardingSubmission for partner ${partnerId} of type BUSINESS exists with the id ${trustOnboardingSubmissionId}"
    )
    void initStateOnboardingSubmissionWithIdIsPresent(Map<String, String> stateParameters) {
        setSystemSecurityContext(); // so AuditorAware is happy
        makeSureTrustOnboardingSubmissionExists(
            UUID.fromString(stateParameters.get("trustOnboardingSubmissionId")),
            UUID.fromString(stateParameters.get("partnerId"))
        );
    }

    private void makeSureTrustOnboardingSubmissionExists(UUID trustOnboardingSubmissionId, UUID partnerId) {
        if (businessPartnerRepository.findById(partnerId).isEmpty()) {
            businessPartnerRepository.save(businessPartnerOfTypeBusiness(partnerId));
        }
        if (submissionRepository.findById(trustOnboardingSubmissionId).isEmpty()) {
            submissionRepository.save(trustOnboardingSubmission(trustOnboardingSubmissionId, partnerId, SUBMITTED));
        }
    }
}
