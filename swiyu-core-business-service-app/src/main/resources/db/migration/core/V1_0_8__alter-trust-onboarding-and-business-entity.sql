ALTER TABLE trust_onboarding_submission
    ADD corresponding_language SMALLINT;

ALTER TABLE trust_onboarding_submission
    ADD proof_of_possession JSONB;

ALTER TABLE trust_onboarding_submission
    ADD submitted_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE business_entity
    ADD is_verified BOOLEAN DEFAULT FALSE;

ALTER TABLE business_entity
    ADD payed_for_did_slots INTEGER DEFAULT 0;

ALTER TABLE business_entity
    ADD payed_for_trust_verification BOOLEAN DEFAULT FALSE;

ALTER TABLE business_entity
    ADD version BIGINT DEFAULT 0;

ALTER TABLE business_entity
    ALTER COLUMN is_verified SET NOT NULL;

ALTER TABLE business_entity
    ALTER COLUMN payed_for_did_slots SET NOT NULL;

ALTER TABLE business_entity
    ALTER COLUMN payed_for_trust_verification SET NOT NULL;

ALTER TABLE business_entity
    ALTER COLUMN version SET NOT NULL;