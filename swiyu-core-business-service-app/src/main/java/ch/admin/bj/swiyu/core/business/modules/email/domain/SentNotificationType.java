package ch.admin.bj.swiyu.core.business.modules.email.domain;

/**
 * Channel a notification was sent through.
 *
 * <p>Only {@code EMAIL} today. The column exists so a later channel - SMS, push - can share the same
 * table and the same idempotence handling instead of getting one of its own.
 */
public enum SentNotificationType {
    EMAIL,
}
