ALTER TABLE trust_onboarding_submission
    ADD initiated_at TIMESTAMP WITHOUT TIME ZONE NULL;

UPDATE trust_onboarding_submission
SET initiated_at = created_at;

ALTER TABLE trust_onboarding_submission
    ALTER COLUMN initiated_at SET NOT NULL;
