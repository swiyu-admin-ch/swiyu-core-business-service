CREATE TABLE vc_entity
(
    id         bigint GENERATED ALWAYS AS IDENTITY,
    base_id    uuid  NOT NULL,
    vc_type    varchar(50),
    decoded_vc text  NULL,
    encoded_vc jsonb NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_vc_entity_datastore_entity_base
        FOREIGN KEY (base_id)
            REFERENCES datastore_entity (id)
);
CREATE INDEX idx_vc_list
    ON vc_entity USING HASH (base_id);
CREATE UNIQUE INDEX idx_vc_search_authoring
    ON vc_entity (base_id, vc_type);
