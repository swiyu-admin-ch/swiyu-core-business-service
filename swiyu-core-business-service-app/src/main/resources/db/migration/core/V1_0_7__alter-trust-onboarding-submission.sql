ALTER TABLE trust_onboarding_submission
    ADD COLUMN reject_reason  VARCHAR(255),
    ADD COLUMN decline_reason VARCHAR(255),
    ADD COLUMN partner_note   VARCHAR(255);