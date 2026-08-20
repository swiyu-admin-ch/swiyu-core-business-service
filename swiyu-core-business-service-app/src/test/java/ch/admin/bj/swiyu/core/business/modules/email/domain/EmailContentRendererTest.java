package ch.admin.bj.swiyu.core.business.modules.email.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.modules.email.config.MailConfig;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

class EmailContentRendererTest {

    /**
     * The only two variables the reviewed templates use, per the feature specification.
     */
    private static final Map<String, Object> COMPLETE_VARIABLES = Map.of(
        "contactEmail",
        "registries@swiyu.admin.ch",
        "servicePortalPartnerUrl",
        "https://portal.trust-infra.swiyu.admin.ch/ui/business-partners/a-partner-id"
    );

    private EmailContentRenderer renderer;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        renderer = new EmailContentRenderer(new MailConfig().emailTemplateEngine());
        logAppender = new ListAppender<>();
        logAppender.start();
        logger = (Logger) LoggerFactory.getLogger(EmailContentRenderer.class);
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EmailType.class)
    void rendersEveryTemplateWithoutLoggingAnError(EmailType emailType) {
        var email = renderer.render(emailType, COMPLETE_VARIABLES, "");

        assertThat(email.subject()).isNotBlank();
        assertThat(email.plainTextMessage()).isNotBlank();
        assertThat(errorMessages()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EmailType.class)
    void everyTemplateDeclaresOnlyTheTwoKnownVariables(EmailType emailType) {
        assertThat(renderer.declaredVariables(emailType)).containsExactlyInAnyOrderElementsOf(
            COMPLETE_VARIABLES.keySet()
        );
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EmailType.class)
    void everyTemplateHasAllFourLanguageSections(EmailType emailType) {
        var email = renderer.render(emailType, COMPLETE_VARIABLES, "");

        // Body order per the feature specification: DE, FR, IT, EN
        assertThat(email.plainTextMessage()).containsSubsequence("Guten Tag", "Bonjour", "Buongiorno", "Hello");
    }

    @Test
    void prependsTheStageMarkerToTheSubject() {
        var email = renderer.render(EmailType.SUBMISSION_ACCEPTED, COMPLETE_VARIABLES, "[REF]");

        assertThat(email.subject()).isEqualTo(
            "[REF] Antrag eingereicht/ Application submitted/ Demande déposée/ Richiesta presentata"
        );
    }

    @Test
    void leavesTheSubjectUnchangedOnProdWhereNoPrefixIsConfigured() {
        var email = renderer.render(EmailType.SUBMISSION_ACCEPTED, COMPLETE_VARIABLES, "");

        assertThat(email.subject()).isEqualTo(
            "Antrag eingereicht/ Application submitted/ Demande déposée/ Richiesta presentata"
        );
    }

    @Test
    void doesNotDuplicateTheSpaceOfAPrefixConfiguredWithOne() {
        var email = renderer.render(EmailType.SUBMISSION_ACCEPTED, COMPLETE_VARIABLES, "[ABN-PREVIEW] ");

        assertThat(email.subject()).startsWith("[ABN-PREVIEW] Antrag");
    }

    @Test
    void stripsTheFrontMatterFromTheBody() {
        var email = renderer.render(EmailType.SUBMISSION_ACCEPTED, COMPLETE_VARIABLES, "");

        assertThat(email.plainTextMessage()).doesNotContain("subject:").startsWith("Guten Tag");
    }

    @Test
    void logsAnErrorWhenAVariableIsNotSupplied() {
        renderer.render(EmailType.TRUST_REGISTRATION_REJECTED, Map.of("contactEmail", "a@b.ch"), "");

        assertThat(errorMessages()).anyMatch(message -> message.contains("servicePortalPartnerUrl"));
    }

    private List<String> errorMessages() {
        return logAppender.list
            .stream()
            .filter(event -> event.getLevel() == Level.ERROR)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}
