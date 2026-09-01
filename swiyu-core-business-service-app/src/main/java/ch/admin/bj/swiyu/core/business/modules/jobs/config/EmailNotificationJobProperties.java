package ch.admin.bj.swiyu.core.business.modules.jobs.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Operational settings of the nightly reminder job.
 *
 * <p>Lives next to the job and not next to the email module: the job owns its schedule, and it is the
 * job that reads these values and hands them to the service.
 *
 * <p>Only the two knobs that are operational live here. The reminder points themselves - 180 / 150 /
 * 120 / 90 / 30 days before expiry, and 6 days of review delay - are not configurable: they come from
 * the feature, and the reviewed text of the first reminder spells its 180 day period out rather than
 * interpolating it. A stage that set them to something else would contradict the wording, so they stay
 * constants.
 *
 * @param window   how far back from a reminder day the job still picks a partner up. Covers nights the
 *                 job did not run - a deployment, an outage, a pod that was down. Must stay well below
 *                 the 30 day gap between two reminder points, otherwise two windows overlap.
 * @param pageSize how many candidates are loaded at once
 */
@Validated
@ConfigurationProperties(prefix = "app.jobs.email-notification")
public record EmailNotificationJobProperties(@NotNull Duration window, @Positive int pageSize) {}
