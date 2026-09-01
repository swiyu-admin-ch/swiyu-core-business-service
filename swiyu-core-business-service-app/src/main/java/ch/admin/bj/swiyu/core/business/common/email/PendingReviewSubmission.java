package ch.admin.bj.swiyu.core.business.common.email;

import java.util.UUID;

/**
 * A trust onboarding submission that has been waiting for review longer than the configured period.
 *
 *
 * @param submissionId id of the submission, part of the reminder's idempotence id so that a partner who
 *                     submits twice is reminded about each submission separately
 * @param partnerId    the business partner to notify
 */
public record PendingReviewSubmission(UUID submissionId, UUID partnerId) {}
