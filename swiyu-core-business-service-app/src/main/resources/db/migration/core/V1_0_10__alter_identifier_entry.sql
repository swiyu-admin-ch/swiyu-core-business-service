ALTER TABLE identifier_entry ADD COLUMN status varchar(255);

UPDATE identifier_entry
SET status = 'NOT_INITIALIZED'
WHERE upload_count = 0 AND status IS NULL;

UPDATE identifier_entry
SET status = 'INITIALIZED'
WHERE upload_count > 0 AND status IS NULL;

ALTER TABLE identifier_entry ALTER COLUMN status SET NOT NULL;