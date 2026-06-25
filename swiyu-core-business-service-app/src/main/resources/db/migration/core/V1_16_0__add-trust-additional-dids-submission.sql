CREATE TABLE trust_additional_dids_submission
(
    id               UUID         NOT NULL PRIMARY KEY,
    partner_id       UUID         NOT NULL,
    version          BIGINT       NOT NULL,
    permission_did   JSONB        NOT NULL,
    dids_to_add      JSONB        NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    reject_reason    VARCHAR(255),
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    created_by       VARCHAR(255),
    last_modified_at TIMESTAMP WITHOUT TIME ZONE,
    last_modified_by VARCHAR(255)
);
