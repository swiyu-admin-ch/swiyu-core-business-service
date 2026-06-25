CREATE TABLE partner_document
(
    id                             UUID PRIMARY KEY,
    partner_id                     UUID                        NOT NULL,
    trust_onboarding_submission_id UUID                        NULL,
    virus_scan_id                  TEXT                        NULL,
    submitted_at                   TIMESTAMP                   NOT NULL,
    media_type                     varchar(255)                NOT NULL,
    version                        BIGINT                      NOT NULL,
    type                           varchar(50)                 NOT NULL,
    file_name                      TEXT                        NOT NULL,
    storage_object_key             TEXT                        NOT NULL,
    -- AuditMetadata fields
    last_modified_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_by               TEXT                        NOT NULL,
    created_at                     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by                     TEXT                        NOT NULL,

    CONSTRAINT fk_partner_document__partner_id
        FOREIGN KEY (partner_id) REFERENCES business_entity (id),
    CONSTRAINT fk_partner_document__trust_onboarding_submission_id
        FOREIGN KEY (trust_onboarding_submission_id) REFERENCES trust_onboarding_submission (id)
);

CREATE INDEX idx_partner_document__trust_submission_id__type
    ON partner_document (trust_onboarding_submission_id, type);

CREATE INDEX idx_partner_document__partner_id
    ON partner_document (partner_id);

CREATE INDEX idx_partner_document__trust_submission_id
    ON partner_document (trust_onboarding_submission_id);

