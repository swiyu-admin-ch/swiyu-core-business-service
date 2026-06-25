ALTER TABLE trust_onboarding_submission
    ADD signatories JSONB;

ALTER TABLE trust_onboarding_submission
    ADD signing_rule VARCHAR(255);