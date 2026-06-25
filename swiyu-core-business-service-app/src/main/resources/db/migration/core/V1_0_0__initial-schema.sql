-- Naming convention for INDEX:
-- https://stackoverflow.com/questions/4107915/postgresql-default-constraint-names/4108266#4108266
CREATE TABLE business_entity
(
    id      uuid NOT NULL,
    name    text NOT NULL,
    contact text NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE status_list_entry
(
    created_by               varchar(255) NOT NULL,
    created_at               timestamp    NOT NULL,
    last_modified_by         varchar(255) NOT NULL,
    last_modified_at         timestamp    NOT NULL,

    status_registry_entry_id uuid         NOT NULL,
    business_entity_id       uuid         NOT NULL,
    upload_count             int          NOT NULL,

    PRIMARY KEY (status_registry_entry_id),
    CONSTRAINT status_list_entry_business_object_entity_id_fkey
        FOREIGN KEY (business_entity_id)
            REFERENCES business_entity (id)
            ON DELETE CASCADE
);

CREATE TABLE identifier_entry
(
    created_by         varchar(255) NOT NULL,
    created_at         timestamp    NOT NULL,
    last_modified_by   varchar(255) NOT NULL,
    last_modified_at   timestamp    NOT NULL,

    id                 uuid         NOT NULL,
    business_entity_id uuid         NOT NULL,
    upload_count       int          NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT identifier_entry_business_object_entity_id_fkey
        FOREIGN KEY (business_entity_id)
            REFERENCES business_entity (id)
            ON DELETE CASCADE
);

