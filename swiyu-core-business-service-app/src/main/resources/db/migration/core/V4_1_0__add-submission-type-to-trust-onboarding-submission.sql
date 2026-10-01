-- Add submission type column to trust_onboarding_submission table
ALTER TABLE trust_onboarding_submission
ADD COLUMN type VARCHAR(64) NOT NULL DEFAULT 'REGISTRATION';