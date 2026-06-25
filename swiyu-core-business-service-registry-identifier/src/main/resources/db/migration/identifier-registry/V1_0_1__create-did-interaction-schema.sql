CREATE TABLE did_entity
(
    id        bigint GENERATED ALWAYS AS IDENTITY,
    base_id   uuid NOT NULL,
    file_type varchar(50),
    scid      varchar(5000),
    read_uri  varchar(500),
    content   text NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_base
        FOREIGN KEY (base_id)
            REFERENCES datastore_entity (id)

);
CREATE INDEX idx_did_list
    ON did_entity USING HASH (base_id);
CREATE UNIQUE INDEX idx_did_search_authoring
    ON did_entity (base_id, file_type);
CREATE UNIQUE INDEX idx_did_search_data
    ON did_entity (base_id, scid, file_type);
