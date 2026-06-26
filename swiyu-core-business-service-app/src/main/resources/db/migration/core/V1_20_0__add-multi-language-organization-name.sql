ALTER TABLE business_entity
    ADD COLUMN entity_name JSONB;

UPDATE business_entity
SET entity_name = jsonb_strip_nulls(jsonb_build_object(
    'default', name
))
WHERE entity_name IS NULL;

ALTER TABLE business_entity
    DROP COLUMN name;

ALTER TABLE trust_onboarding_submission
    ADD COLUMN entity_name JSONB;

UPDATE trust_onboarding_submission
SET entity_name = jsonb_strip_nulls(jsonb_build_object(
    'default', CASE corresponding_language
        WHEN 0 THEN entity_name_en
        WHEN 1 THEN entity_name_de
        WHEN 2 THEN entity_name_fr
        WHEN 3 THEN entity_name_it
        WHEN 4 THEN entity_name_rm
        ELSE COALESCE(entity_name_de, entity_name_fr, entity_name_it, entity_name_en, entity_name_rm)
    END,
    'de-CH', entity_name_de,
    'fr-CH', entity_name_fr,
    'it-CH', entity_name_it,
    'en-CH', entity_name_en,
    'rm-CH', entity_name_rm
))
WHERE entity_name IS NULL;

ALTER TABLE trust_onboarding_submission
    DROP COLUMN entity_name_de,
    DROP COLUMN entity_name_fr,
    DROP COLUMN entity_name_it,
    DROP COLUMN entity_name_en,
    DROP COLUMN entity_name_rm;
