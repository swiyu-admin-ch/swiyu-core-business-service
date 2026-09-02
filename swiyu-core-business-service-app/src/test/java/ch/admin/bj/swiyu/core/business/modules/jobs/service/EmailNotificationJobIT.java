package ch.admin.bj.swiyu.core.business.modules.jobs.service;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.activeBusinessPartnerIdentityValidUntil;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.businessPartnerOfTypeGov;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.deactivatedBusinessPartnerIdentity;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.submittedSince;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bj.swiyu.core.business.modules.email.domain.EmailType;
import ch.admin.bj.swiyu.core.business.modules.email.domain.SentNotification;
import ch.admin.bj.swiyu.core.business.modules.email.domain.SentNotificationRepository;
import ch.admin.bj.swiyu.core.business.modules.email.domain.SentNotificationType;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * The nightly reminder job against real partners and real submissions.
 *
 * <p>Asserts on the transactional outbox rather than on a mocked publisher: what this story has to get
 * right is <em>which</em> reminders end up there and, above all, that a second run adds none.
 *
 * <p>{@code app.jobs.email-notification.page-size} is 2 in the test profile, so every case with three
 * candidates also proves that the job walks past the first page.
 */
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
@WithExtendedJeapAuthenticationToken
class EmailNotificationJobIT {

    private static final int WINDOW_DAYS = 4;

    @Autowired
    EmailNotificationJob job;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @Autowired
    TestRepositories repos;

    @Autowired
    SentNotificationRepository sentNotifications;

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        repos.truncateTables();
        sentNotifications.deleteAll();
        // The outbox repository is the framework's own and brings no transaction of its own
        transactionTemplate.executeWithoutResult(status -> {
            var messages = deferredMessageRepository.findAll();
            deferredMessageRepository.deleteAllById(
                messages.stream().map(DeferredMessage::getId).collect(Collectors.toSet())
            );
        });
    }

    @Test
    void publishesARenewalReminderForEveryReminderPointOfTheFeature() {
        // one partner per reminder point, each exactly that many days from expiry
        List.of(180, 150, 120, 90, 30).forEach(this::partnerExpiringInDays);

        job.triggerScheduledEmailNotifications();

        assertThat(publishedEmailCount()).isEqualTo(5);
    }

    @Test
    void publishesTheInitialReminderUnderItsOwnEmailType() {
        partnerExpiringInDays(180);
        partnerExpiringInDays(150);

        job.triggerScheduledEmailNotifications();

        // 180 days gets the differently worded first reminder, the later points share one type
        assertThat(publishedIdempotenceIds()).anyMatch(id ->
            id.startsWith(EmailType.TRUST_RENEWAL_REMINDER_INITIAL.name())
        );
        assertThat(publishedIdempotenceIds()).anyMatch(id ->
            id.startsWith(EmailType.TRUST_RENEWAL_REMINDER.name() + "-")
        );
    }

    /**
     * The reason this story exists. Every night of the window has to produce the same idempotence id -
     * a generated one would mean one email per night for as long as the window lasts.
     */
    @Test
    void buildsTheSameIdempotenceIdOnEveryNightOfTheWindow() {
        partnerExpiringInDays(180);

        job.triggerScheduledEmailNotifications();
        job.triggerScheduledEmailNotifications();

        assertThat(publishedIdempotenceIds()).hasSize(2).containsOnly(publishedIdempotenceIds().getFirst());
    }

    /**
     * The second half of the guard: once the email has actually gone out, the job stops publishing at
     * all instead of leaving it to the consumer to drop the command.
     */
    @Test
    void publishesNothingOnceTheReminderWasRecordedAsSent() {
        partnerExpiringInDays(180);
        job.triggerScheduledEmailNotifications();
        recordAsSent(publishedIdempotenceIds().getFirst());

        job.triggerScheduledEmailNotifications();

        assertThat(publishedEmailCount()).isEqualTo(1);
    }

    /**
     * The edge case from the story: a night the job did not run must not drop that day's reminders.
     */
    @Test
    void picksUpAPartnerTheJobMissedOnTheExactDay() {
        partnerExpiringInDays(180 - WINDOW_DAYS + 1);

        job.triggerScheduledEmailNotifications();

        assertThat(publishedEmailCount()).isEqualTo(1);
    }

    @Test
    void ignoresAPartnerOutsideTheWindow() {
        partnerExpiringInDays(180 - WINDOW_DAYS - 1); // just fell through
        partnerExpiringInDays(200); // not due yet
        partnerExpiringInDays(60); // between two reminder points

        job.triggerScheduledEmailNotifications();

        assertThat(publishedEmailCount()).isZero();
    }

    @Test
    void ignoresAPartnerWhoseIdentityIsNoLongerActive() {
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        partner.applyBusinessPartnerIdentityEvent(deactivatedBusinessPartnerIdentity());
        repos.businessPartner.save(partner);

        job.triggerScheduledEmailNotifications();

        assertThat(publishedEmailCount()).isZero();
    }

    @Test
    void publishesAReviewDelayedReminderOnceTheSubmissionHasWaitedSixDays() {
        submittedDaysAgo(7);

        job.triggerScheduledEmailNotifications();

        assertThat(publishedIdempotenceIds())
            .singleElement()
            .asString()
            .startsWith(EmailType.TRUST_REVIEW_DELAYED.name());
    }

    @Test
    void ignoresASubmissionThatHasNotWaitedLongEnough() {
        submittedDaysAgo(3);

        job.triggerScheduledEmailNotifications();

        assertThat(publishedEmailCount()).isZero();
    }

    @Test
    void ignoresASubmissionThatIsNoLongerWaitingForReview() {
        var submission = submittedDaysAgo(7);
        submission.markAsInformationRequested("something missing");
        repos.trustOnboardingSubmission.save(submission);

        job.triggerScheduledEmailNotifications();

        assertThat(publishedEmailCount()).isZero();
    }

    /**
     * What the consumer of {@code TiSendEmailCommand} writes once the gateway has taken the email.
     */
    private void recordAsSent(String idempotenceId) {
        sentNotifications.save(
            new SentNotification(
                idempotenceId,
                SentNotificationType.EMAIL,
                UUID.randomUUID(),
                JsonNodeFactory.instance.objectNode()
            )
        );
    }

    private void partnerExpiringInDays(int days) {
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        // Half a day in, so the partner sits inside the day rather than exactly on its boundary
        var validUntil = Instant.now().plus(Duration.ofDays(days)).minus(Duration.ofHours(12));
        partner.applyBusinessPartnerIdentityEvent(activeBusinessPartnerIdentityValidUntil(validUntil));
        repos.businessPartner.save(partner);
    }

    private TrustOnboardingSubmission submittedDaysAgo(int days) {
        var partner = repos.businessPartner.save(businessPartnerOfTypeGov(UUID.randomUUID()));
        var submission = submittedSince(UUID.randomUUID(), partner.getId(), Instant.now().minus(Duration.ofDays(days)));
        return repos.trustOnboardingSubmission.save(submission);
    }

    private long publishedEmailCount() {
        return publishedIdempotenceIds().size();
    }

    private List<String> publishedIdempotenceIds() {
        return deferredMessageRepository.findAll().stream().map(DeferredMessage::getMessageIdempotenceId).toList();
    }
}
