CREATE TABLE vqps_submission
(
    id                            UUID         NOT NULL PRIMARY KEY,
    partner_id                    UUID         NOT NULL,
    version                       BIGINT       NOT NULL,
    status                        VARCHAR(255) NOT NULL,
    sub                           TEXT         NOT NULL,
    purpose_name                  JSONB        NOT NULL,
    purpose_description           JSONB        NOT NULL,
    scope                         TEXT         NOT NULL,
    query                         JSONB        NOT NULL,
    publication_failure_reason    VARCHAR(255),
    publication_result_jti        UUID,
    publication_result_jwt        TEXT,
    publication_result_expires_at TIMESTAMP WITHOUT TIME ZONE,
    created_at                    TIMESTAMP WITHOUT TIME ZONE,
    created_by                    VARCHAR(255),
    last_modified_at              TIMESTAMP WITHOUT TIME ZONE,
    last_modified_by              VARCHAR(255)
);
