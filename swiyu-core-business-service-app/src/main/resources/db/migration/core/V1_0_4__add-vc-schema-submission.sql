CREATE TABLE vc_schema_submission
(
    id               UUID         NOT NULL,
    partner_id       UUID         NOT NULL,
    version          BIGINT       NOT NULL,
    status           SMALLINT     NOT NULL,
    file             TEXT         NOT NULL,
    failure_reason   TEXT,
    last_modified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_by VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by       VARCHAR(255) NOT NULL,
    CONSTRAINT vc_schema_submission_id_pk PRIMARY KEY (id)
);