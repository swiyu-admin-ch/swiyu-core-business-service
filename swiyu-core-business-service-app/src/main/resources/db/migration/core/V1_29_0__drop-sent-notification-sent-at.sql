-- Drops sent_notification.sent_at (EID-6628).
--
-- The column carried no information that created_at did not already carry: it was set to Instant.now()
-- while building the row, and the auditing listener stamps created_at in the same persist. Nothing ever
-- read it. Callers that want to know when a notification went out use auditMetadata.createdAt.
ALTER TABLE sent_notification
    DROP COLUMN sent_at;
