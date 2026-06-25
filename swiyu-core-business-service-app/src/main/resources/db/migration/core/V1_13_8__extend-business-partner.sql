ALTER TABLE business_entity
    ADD city VARCHAR(255);

ALTER TABLE business_entity
    ADD contact_phone VARCHAR(255);

ALTER TABLE business_entity
    ADD country VARCHAR(255);

ALTER TABLE business_entity
    ADD postal_code VARCHAR(255);

ALTER TABLE business_entity
    ADD region VARCHAR(255);

ALTER TABLE business_entity
    ADD street VARCHAR(255);

ALTER TABLE business_entity
    ADD uid VARCHAR(255);

ALTER TABLE trust_onboarding_submission
    ADD region VARCHAR(255);

ALTER TABLE business_entity
    RENAME COLUMN contact TO contact_email;

ALTER TABLE trust_onboarding_submission
    ADD entity_region VARCHAR(255);