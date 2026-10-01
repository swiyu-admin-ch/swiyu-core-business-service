-- First hard-delete safeguard. While hard_delete_allowed is false, CBS refuses every
-- hard delete of the partner regardless of the caller. Governmental institutions are locked to
-- false; all other partners start armed (true) and can be toggled by ops via the internal API.
ALTER TABLE business_entity
    ADD COLUMN hard_delete_allowed BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE business_entity
SET hard_delete_allowed = FALSE
WHERE type = 'GOVERNMENTAL_INSTITUTION';
