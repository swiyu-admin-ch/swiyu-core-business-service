CREATE SEQUENCE IF NOT EXISTS deferred_message_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE deferred_message
(
    id                     BIGINT NOT NULL,
    message                BYTEA,
    key                    BYTEA,
    cluster_name           VARCHAR(255),
    topic                  VARCHAR(255),
    message_id             VARCHAR(255),
    message_idempotence_id VARCHAR(255),
    message_type_name      VARCHAR(255),
    message_type_version   VARCHAR(255),
    created                TIMESTAMP WITHOUT TIME ZONE,
    send_immediately       BOOLEAN,
    schedule_after         TIMESTAMP WITHOUT TIME ZONE,
    sent_immediately       TIMESTAMP WITHOUT TIME ZONE,
    sent_scheduled         TIMESTAMP WITHOUT TIME ZONE,
    failed                 TIMESTAMP WITHOUT TIME ZONE,
    fail_reason            VARCHAR(255),
    resend                 BOOLEAN,
    trace_id_high          BIGINT,
    trace_id               BIGINT,
    span_id                BIGINT,
    parent_span_id         BIGINT,
    trace_id_string        VARCHAR(255),
    CONSTRAINT pk_deferred_message PRIMARY KEY (id)
);

CREATE TABLE idempotent_processing
(
    created_at             TIMESTAMP WITHOUT TIME ZONE,
    idempotence_id         VARCHAR(255) NOT NULL,
    idempotence_id_context VARCHAR(255) NOT NULL,
    CONSTRAINT pk_idempotent_processing PRIMARY KEY (idempotence_id, idempotence_id_context)
);

CREATE TABLE shedlock (
                          name                VARCHAR(64)                 NOT NULL,
                          lock_until          TIMESTAMP                   NOT NULL,
                          locked_at           TIMESTAMP                   NOT NULL,
                          locked_by           VARCHAR(255)                NOT NULL,
                          PRIMARY KEY (name)
);