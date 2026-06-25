-- audit metadata previously set 'null' when call was made through api gw without preferredUsername
UPDATE did_entity
SET
    created_by = 'system',
    last_modified_by = 'system'
WHERE
    created_by = 'null'
   OR last_modified_by = 'null';

UPDATE datastore_entity
SET
    created_by = 'system',
    last_modified_by = 'system'
WHERE
    created_by = 'null'
   OR last_modified_by = 'null';