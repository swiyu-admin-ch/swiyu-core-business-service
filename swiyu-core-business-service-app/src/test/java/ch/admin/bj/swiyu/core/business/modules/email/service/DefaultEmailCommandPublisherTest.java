package ch.admin.bj.swiyu.core.business.modules.email.service;

import static ch.admin.bj.swiyu.core.business.modules.email.domain.EmailType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.modules.email.config.MailConfig;
import ch.admin.bj.swiyu.core.business.modules.email.config.MailProperties;
import ch.admin.bj.swiyu.core.business.modules.email.domain.EmailContentRenderer;
import ch.admin.bj.swiyu.core.business.modules.email.domain.EmailType;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.messagetype.ti.TiSendEmailCommand;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

/**
 * Builds every {@link EmailType} through the publisher and asserts that the resulting command
 * carries a complete four language email with no unresolved template variables left in it.
 */
class DefaultEmailCommandPublisherTest {

    private static final UUID PARTNER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID SUBMISSION_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final Instant VALID_UNTIL = Instant.parse("2027-03-01T00:00:00Z");
    private static final String RECIPIENT = "contact.person@partner.example.com";
    private static final String FROM = "registries@swiyu.admin.ch";
    private static final String REPLY_TO = "registries@swiyu.admin.ch";
    private static final String SUBJECT_PREFIX = "[DEV]";
    private static final String BASE_URL = "https://portal.trust-infra.swiyu.admin.ch";
    private static final String PORTAL_URL = BASE_URL + "/ui/business-partners/" + PARTNER_ID;

    /**
     * Anything that still looks like a template expression after rendering.
     */
    private static final Pattern UNRESOLVED = Pattern.compile("\\$\\{[^}]*}|\\[\\[[^\\]]*]]|\\[\\([^)]*\\)]");

    private static final List<String> GREETINGS = List.of("Guten Tag", "Bonjour", "Buongiorno", "Hello");

    private TransactionalOutbox outbox;
    private BusinessPartnerService businessPartnerService;
    private SentNotificationService sentNotificationService;
    private DefaultEmailCommandPublisher publisher;

    @BeforeEach
    void setUp() {
        outbox = mock(TransactionalOutbox.class);
        businessPartnerService = mock(BusinessPartnerService.class);
        when(businessPartnerService.getContactEmail(PARTNER_ID)).thenReturn(RECIPIENT);
        sentNotificationService = mock(SentNotificationService.class);
        publisher = publisherWithEmailFunctionality(true);
    }

    private DefaultEmailCommandPublisher publisherWithEmailFunctionality(boolean emailEnabled) {
        var servicePortal = new MailProperties.ServicePortal();
        servicePortal.setBaseUrl(BASE_URL);
        servicePortal.setPartnerTemplateUrl(BASE_URL + "/ui/business-partners/{0}");

        var mailProperties = new MailProperties();
        mailProperties.setFrom(FROM);
        mailProperties.setReplyTo(REPLY_TO);
        mailProperties.setSubjectPrefix(SUBJECT_PREFIX);
        mailProperties.setServicePortal(servicePortal);

        var renderer = new EmailContentRenderer(new MailConfig().emailTemplateEngine());
        return new DefaultEmailCommandPublisher(
            outbox,
            renderer,
            mailProperties,
            new FunctionalityProperties(emailEnabled),
            businessPartnerService,
            sentNotificationService
        );
    }

    @BeforeAll
    static void installAvroClassWhitelist() {
        AvroClassSecurity.installDefaultIfMissing();
    }

    static Stream<Arguments> allEmailTypes() {
        return Stream.of(
            Arguments.of(SUBMISSION_ACCEPTED, byPartnerId(EmailCommandPublisher::submissionAccepted)),
            Arguments.of(TRUST_REGISTRATION_SUCCEEDED, byPartnerId(EmailCommandPublisher::trustRegistrationSucceeded)),
            Arguments.of(TRUST_REGISTRATION_REJECTED, byPartnerId(EmailCommandPublisher::trustRegistrationRejected)),
            Arguments.of(
                TRUST_REGISTRATION_INFORMATION_REQUESTED,
                byPartnerId(EmailCommandPublisher::trustRegistrationInformationRequested)
            ),
            Arguments.of(
                TRUST_PROFILE_CHANGE_SUCCEEDED,
                byPartnerId(EmailCommandPublisher::trustProfileChangeSucceeded)
            ),
            Arguments.of(TRUST_PROFILE_CHANGE_REJECTED, byPartnerId(EmailCommandPublisher::trustProfileChangeRejected)),
            Arguments.of(
                TRUST_PROFILE_CHANGE_INFORMATION_REQUESTED,
                byPartnerId(EmailCommandPublisher::trustProfileChangeInformationRequested)
            ),
            Arguments.of(TRUST_RENEWAL_SUCCEEDED, byPartnerId(EmailCommandPublisher::trustRenewalSucceeded)),
            Arguments.of(TRUST_IDENTITY_EXPIRED, byPartnerId(EmailCommandPublisher::trustIdentityExpired)),
            Arguments.of(TRUST_ADDITIONAL_DID_ADDED, byPartnerId(EmailCommandPublisher::trustAdditionalDidAdded)),
            Arguments.of(
                TRUST_PROTECTED_VERIFICATION_AHV_APPROVED,
                byPartnerId(EmailCommandPublisher::trustProtectedVerificationAhvApproved)
            ),
            Arguments.of(
                TRUST_PROTECTED_VERIFICATION_AHV_REJECTED,
                byPartnerId(EmailCommandPublisher::trustProtectedVerificationAhvRejected)
            ),
            Arguments.of(TRUST_REVIEW_DELAYED, scheduled(p -> p.trustReviewDelayed(PARTNER_ID, SUBMISSION_ID, 6))),
            Arguments.of(
                TRUST_RENEWAL_REMINDER_INITIAL,
                scheduled(p -> p.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 180))
            ),
            Arguments.of(TRUST_RENEWAL_REMINDER, scheduled(p -> p.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 90)))
        );
    }

    private static Consumer<EmailCommandPublisher> byPartnerId(BiConsumer<EmailCommandPublisher, UUID> method) {
        return publisher -> method.accept(publisher, PARTNER_ID);
    }

    private static Consumer<EmailCommandPublisher> scheduled(Consumer<EmailCommandPublisher> method) {
        return method;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allEmailTypes")
    void publishesCompleteEmailWithoutUnresolvedVariables(
        EmailType emailType,
        Consumer<EmailCommandPublisher> trigger
    ) {
        trigger.accept(publisher);

        var payload = capturePublishedCommand().getPayload();

        assertThat(payload.getPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(payload.getEmailType()).isEqualTo(emailType.name());
        assertThat(payload.getTo()).containsExactly(RECIPIENT);
        assertThat(payload.getFrom()).isEqualTo(FROM);
        assertThat(payload.getReplyTo()).isEqualTo(REPLY_TO);
        assertThat(payload.getSentAt()).isNotNull();

        // Stage marker plus the subject in all four languages, separated by "/"
        assertThat(payload.getSubject()).startsWith(SUBJECT_PREFIX + " ").contains("/");

        // Every language section, the portal link and the sender address must have been resolved
        var body = payload.getPlainTextMessage();
        assertThat(body).contains(GREETINGS).contains(PORTAL_URL).contains(FROM);

        assertNoUnresolvedVariables(emailType, payload.getSubject());
        assertNoUnresolvedVariables(emailType, body);
    }

    @Test
    void coversEveryEmailType() {
        var covered = allEmailTypes()
            .map(arguments -> arguments.get()[0])
            .toList();
        assertThat(covered).containsExactlyInAnyOrder((Object[]) EmailType.values());
    }

    @Test
    void resolvesThePortalLinkOncePerLanguage() {
        publisher.submissionAccepted(PARTNER_ID);

        var body = capturePublishedCommand().getPayload().getPlainTextMessage();
        assertThat(countOccurrences(body, PORTAL_URL)).isEqualTo(GREETINGS.size());
    }

    @Test
    void resolvesTheRecipientFromThePartnerSoCallersDoNotHaveTo() {
        publisher.submissionAccepted(PARTNER_ID);

        verify(businessPartnerService).getContactEmail(PARTNER_ID);
        assertThat(capturePublishedCommand().getPayload().getTo()).containsExactly(RECIPIENT);
    }

    @Test
    void publishesNothingWhenTheFunctionalityIsDisabled() {
        var disabledPublisher = publisherWithEmailFunctionality(false);

        disabledPublisher.submissionAccepted(PARTNER_ID);

        verifyNoInteractions(outbox, businessPartnerService);
    }

    @Test
    void publishesNothingWhenThePartnerHasNoContactEmail() {
        when(businessPartnerService.getContactEmail(PARTNER_ID)).thenReturn(null, "  ");

        publisher.submissionAccepted(PARTNER_ID);
        publisher.submissionAccepted(PARTNER_ID);

        verifyNoInteractions(outbox);
    }

    @Test
    void publishesNothingWithoutAPartnerId() {
        publisher.submissionAccepted(null);

        verifyNoInteractions(outbox, businessPartnerService);
    }

    @Test
    void buildsADeterministicIdempotenceIdSoTheSameReminderIsNeverSentTwice() {
        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 180);
        var first = capturePublishedCommand().getIdentity().getIdempotenceId();

        // A second run of the same night must produce the same id, otherwise the partner is reminded
        // every night for as long as the window lasts.
        setUp();
        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 180);
        var second = capturePublishedCommand().getIdentity().getIdempotenceId();

        assertThat(second)
            .isEqualTo(first)
            .contains(PARTNER_ID.toString())
            .startsWith("TRUST_RENEWAL_REMINDER_INITIAL");
    }

    @Test
    void buildsADifferentIdempotenceIdOnceTheIdentityWasRenewed() {
        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 180);
        var beforeRenewal = capturePublishedCommand().getIdentity().getIdempotenceId();

        setUp();
        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL.plus(Duration.ofDays(730)), 180);
        var afterRenewal = capturePublishedCommand().getIdentity().getIdempotenceId();

        // Without the expiry in the id, a renewed partner would never be reminded again.
        assertThat(afterRenewal).isNotEqualTo(beforeRenewal);
    }

    @Test
    void buildsADifferentIdempotenceIdPerReminderStage() {
        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 180);
        var initial = capturePublishedCommand().getIdentity().getIdempotenceId();

        setUp();
        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 150);
        var later = capturePublishedCommand().getIdentity().getIdempotenceId();

        assertThat(later).isNotEqualTo(initial).startsWith("TRUST_RENEWAL_REMINDER-");
    }

    @Test
    void publishesNothingWhenTheReminderAlreadyWentOut() {
        when(sentNotificationService.alreadySent(any())).thenReturn(true);

        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 180);
        publisher.trustReviewDelayed(PARTNER_ID, SUBMISSION_ID, 6);

        verifyNoInteractions(outbox);
    }

    @Test
    void printsThePeriodOfTheReminderInAllFourLanguages() {
        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 90);

        var body = capturePublishedCommand().getPayload().getPlainTextMessage();
        // The number is the same in every language, the noun comes from the template
        assertThat(body).contains("90 Tage", "90 jours", "90 giorni", "90 days");
    }

    @Test
    void statesThePeriodOfTheInitialReminderThatItsTextSpellsOut() {
        publisher.trustRenewalReminder(PARTNER_ID, VALID_UNTIL, 180);

        var body = capturePublishedCommand().getPayload().getPlainTextMessage();
        // The reviewed text of the initial reminder writes the period out instead of interpolating it,
        // so a change to the 180 day trigger would silently contradict the wording. This is the test
        // that catches it - there is no unresolved variable to give it away.
        assertThat(body).contains("180 Tage", "180 jours", "180 giorni", "180 days");
    }

    private TiSendEmailCommand capturePublishedCommand() {
        var captor = ArgumentCaptor.forClass(TiSendEmailCommand.class);
        verify(outbox).sendMessage(captor.capture(), any(), eq(TiSendEmailCommand.TypeRef.DEFAULT_TOPIC));
        return captor.getValue();
    }

    private static void assertNoUnresolvedVariables(EmailType emailType, String rendered) {
        var unresolved = UNRESOLVED.matcher(rendered).results().map(MatchResult::group).toList();
        assertThat(unresolved).as("unresolved template variables in email %s", emailType).isEmpty();
    }

    private static int countOccurrences(String haystack, String needle) {
        var count = 0;
        var index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
