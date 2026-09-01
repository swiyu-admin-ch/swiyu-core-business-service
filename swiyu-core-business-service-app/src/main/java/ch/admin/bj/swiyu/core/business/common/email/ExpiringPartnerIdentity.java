package ch.admin.bj.swiyu.core.business.common.email;

import java.time.Instant;
import java.util.UUID;

/**
 * A business partner whose Trust Identity expires within the queried window.
 *
 * <p>A projection and not the business partner entity: the nightly reminder job runs over the whole
 * partner base, and there is no reason to hydrate entities with their jsonb columns to read two fields.
 *
 * @param partnerId  id of the business partner
 * @param validUntil expiry of the Trust Identity, part of the reminder's idempotence id
 */
public record ExpiringPartnerIdentity(UUID partnerId, Instant validUntil) {}
