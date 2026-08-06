CREATE TABLE protected_verification_submission
(
    id                UUID         NOT NULL,
    sbn_id            UUID         NOT NULL,
    partner_id        UUID         NOT NULL,
    entity_name       VARCHAR(255) NOT NULL,
    uid               VARCHAR(255),
    contact_first_name VARCHAR(255),
    contact_last_name  VARCHAR(255),
    contact_email       VARCHAR(255),
    contact_phone       VARCHAR(255),
    contact_corresponding_language VARCHAR(32),
    reason            TEXT         NOT NULL,
    category          VARCHAR(64)  NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    reject_reason     TEXT,
    submitted_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version           BIGINT       NOT NULL,
    last_modified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by       VARCHAR(255) NOT NULL,
    CONSTRAINT protected_verification_submission_id_pk PRIMARY KEY (id)
);

CREATE INDEX idx_protected_verification_submission__partner_id
    ON protected_verification_submission (partner_id);
