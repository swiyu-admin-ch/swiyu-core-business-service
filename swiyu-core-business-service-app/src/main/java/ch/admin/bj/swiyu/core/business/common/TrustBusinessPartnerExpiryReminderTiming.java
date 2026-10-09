package ch.admin.bj.swiyu.core.business.common;

import java.time.Duration;
import java.util.List;

/**
 * Business values for timing relative to an identity's expiry, shared between the reminder job and the
 * partner-expiry window.
 *
 * <p>Deliberately constants, not {@link org.springframework.boot.context.properties.ConfigurationProperties}:
 * the reviewed text of the first reminder spells the 180 day period out, so a stage must not be able to
 * diverge from it. Lives in common so the email and management modules read it without depending on each
 * other.
 */
public final class TrustBusinessPartnerExpiryReminderTiming {

    /** Days before expiry at which a reminder goes out, per the feature. First entry = the "expiring soon" window. */
    public static final List<Integer> REMINDER_DAYS_BEFORE_EXPIRATION = List.of(180, 150, 120, 90, 30);

    /** How far out from expiry a partner counts as "expiring soon". Same as the first reminder point. */
    public static final Duration RE_VERIFICATION_WINDOW = Duration.ofDays(REMINDER_DAYS_BEFORE_EXPIRATION.getFirst());

    private TrustBusinessPartnerExpiryReminderTiming() {}
}
