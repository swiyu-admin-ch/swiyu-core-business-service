-- Update all NULL types to UNKNOWN for existing business entities
UPDATE business_entity
SET type = 'UNKNOWN'
WHERE type IS NULL;

-- Make the type column NOT NULL
ALTER TABLE business_entity
    ALTER COLUMN type SET NOT NULL;

