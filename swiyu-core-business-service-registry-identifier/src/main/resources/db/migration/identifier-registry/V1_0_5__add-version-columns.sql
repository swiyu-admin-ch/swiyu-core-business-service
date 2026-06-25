ALTER TABLE datastore_entity
    ADD COLUMN version int NOT NULL DEFAULT 0;

ALTER TABLE did_entity
    ADD COLUMN version int NOT NULL DEFAULT 0;
