-- Record of every notification email that was handed to the SMTP gateway (EID-6627).
--
-- Answers "which email did this partner receive, and when". Preventing a duplicate send is not this
-- table's job - that is @IdempotentMessageHandler on the processor.
--
-- idempotence_id is TEXT rather than UUID on purpose: today it holds the generated UUID of the
-- command, but the scheduled reminders (EID-6628) will use composed business keys such as
-- {emailType}-{partnerId}-{trustOnboardingSubmissionId}-{delay}.
--
-- The email itself is stored as JSON of the Email record - type, recipients, subject and body are
-- all in there, so they need no columns of their own.
CREATE TABLE sent_notification
(
    id               UUID         NOT NULL,
    idempotence_id   TEXT         NOT NULL,
    type             VARCHAR(32)  NOT NULL,
    partner_id       UUID,
    email            JSONB        NOT NULL,
    sent_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by       VARCHAR(255) NOT NULL,
    CONSTRAINT sent_notification_id_pk PRIMARY KEY (id),
    CONSTRAINT sent_notification_idempotence_id_uq UNIQUE (idempotence_id)
);

CREATE INDEX idx_sent_notification__partner_id
    ON sent_notification (partner_id);
