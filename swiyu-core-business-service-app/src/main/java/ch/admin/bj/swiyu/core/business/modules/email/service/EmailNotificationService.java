package ch.admin.bj.swiyu.core.business.modules.email.service;

import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Finds the partners a scheduled reminder is due for and publishes it.
 *
 * <p>Two reminders, both from the feature: the Trust Identity expires soon, and a submission has been
 * waiting for review too long.
 *
 * <p>Deliberately not {@code @Transactional}. A single transaction over the whole run would mean one
 * unreachable partner rolls back every reminder of that night - and Spring marks a transaction
 * rollback-only as soon as an exception leaves a transactional method, so catching it here would not
 * help. Each publish opens its own transaction instead, and a failure costs exactly one reminder.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    /**
     * Days before expiry at which a reminder goes out, per the feature. The first one gets its own,
     * differently worded email type, whose reviewed text spells the 180 days out instead of
     * interpolating them - the two must be changed together.
     */
    private static final List<Integer> REMINDER_DAYS_BEFORE_EXPIRATION = List.of(180, 150, 120, 90, 30);

    /** Days a submission may wait for review before the partner is told about the delay. */
    private static final int REVIEW_DELAY_DAYS = 6;

    private final BusinessPartnerService businessPartnerService;
    private final TrustOnboardingService trustOnboardingService;
    private final EmailCommandPublisher emailCommandPublisher;
    private final FunctionalityProperties functionalityProperties;

    /**
     * @param window   how far back from a reminder day a partner is still picked up
     * @param pageSize how many candidates are loaded at once
     */
    public void publishDueNotifications(Duration window, int pageSize) {
        // To assert that the lock is held (prevents misconfiguration errors)
        LockAssert.assertLocked();

        // Checked here and not only in the publisher: with the functionality off, the job would
        // otherwise query for candidates every night on every stage and log how many it "checked",
        // which reads as if reminders were going out.
        if (Boolean.FALSE.equals(functionalityProperties.emailEnabled())) {
            log.info("Functionality 'email' is disabled, not looking for due reminders");
            return;
        }

        publishDelayedReviewReminders(pageSize);
        REMINDER_DAYS_BEFORE_EXPIRATION.forEach(days -> publishRenewalReminders(days, window, pageSize));
    }

    private void publishDelayedReviewReminders(int pageSize) {
        var candidates = forEachPage(
            pageable ->
                trustOnboardingService.findSubmissionsPendingReviewLongerThan(
                    Duration.ofDays(REVIEW_DELAY_DAYS),
                    pageable
                ),
            submission ->
                emailCommandPublisher.trustReviewDelayed(
                    submission.partnerId(),
                    submission.submissionId(),
                    REVIEW_DELAY_DAYS
                ),
            "review delayed reminder",
            pageSize
        );
        log.info("Checked {} submissions pending review for longer than {} days", candidates, REVIEW_DELAY_DAYS);
    }

    private void publishRenewalReminders(int daysUntilExpiration, Duration window, int pageSize) {
        // A window and not the exact day: the job runs once a night, and a night it does not run would
        // otherwise drop that day's reminders for good. Sending twice is prevented by the idempotence
        // id, which is the same on every night of the window.
        var windowEnd = Instant.now().plus(Duration.ofDays(daysUntilExpiration));
        var windowStart = windowEnd.minus(window);

        var candidates = forEachPage(
            pageable -> businessPartnerService.findIdentitiesExpiringBetween(windowStart, windowEnd, pageable),
            identity ->
                emailCommandPublisher.trustRenewalReminder(
                    identity.partnerId(),
                    identity.validUntil(),
                    daysUntilExpiration
                ),
            "renewal reminder",
            pageSize
        );
        log.info("Checked {} identities expiring in about {} days", candidates, daysUntilExpiration);
    }

    /**
     * Walks the candidates page by page and hands each one to {@code publish}.
     *
     * <p>Paged rather than a single list because both queries run over data whose size nothing in the
     * code bounds. One failing candidate is logged and skipped so the rest of the night still goes out.
     *
     * <p>Each page is read in its own transaction, so a row that changes mid-run can be seen twice or
     * missed. Seeing it twice is harmless - the idempotence id is the same. Missing it costs one night,
     * and the window picks it up again the next.
     *
     * @return the number of candidates seen - not the number of emails published, which is lower
     *         whenever a reminder had already been sent
     */
    private <T> int forEachPage(Function<Pageable, Page<T>> query, Consumer<T> publish, String what, int pageSize) {
        var pageable = (Pageable) PageRequest.of(0, pageSize);
        var seen = 0;
        Page<T> page;
        do {
            page = query.apply(pageable);
            for (var candidate : page) {
                seen++;
                try {
                    publish.accept(candidate);
                } catch (Exception e) {
                    // Swallowed on purpose: one partner we cannot reach must not stop the batch.
                    log.error("Could not publish {} for {}", what, candidate, e);
                }
            }
            pageable = pageable.next();
        } while (page.hasNext());
        return seen;
    }
}
