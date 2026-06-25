-- Optional: avoid long waits if another session holds a lock
-- SET lock_timeout = '10s';

-- (A) Guard: prevent unmapped codes from slipping through silently.
-- Adjust the NOT IN set to your actual allowed numeric codes.
DO $$
DECLARE
bad_count bigint;
BEGIN
SELECT COUNT(*)
INTO bad_count
FROM trust_onboarding_submission
WHERE status IS NULL
   OR status NOT IN (0, 1, 2, 3, 4);  -- <-- adjust to your actual codes
IF bad_count > 0 THEN
        RAISE EXCEPTION 'Aborting: found % rows with NULL or unexpected status code', bad_count;
END IF;
END$$;

-- (B) If you previously attempted to create an index with the same name, drop it first (no-op if absent).
DROP INDEX IF EXISTS trust_onboarding_submission_partner_id_status_key;

-- (C) Convert SMALLINT -> TEXT by mapping numeric codes to enum names.
ALTER TABLE trust_onboarding_submission
ALTER COLUMN status TYPE text
  USING CASE status
         WHEN 0 THEN 'UNSUBMITTED'
         WHEN 1 THEN 'SUBMITTED'
         WHEN 2 THEN 'SUCCEEDED'
         WHEN 3 THEN 'REJECTED'
         WHEN 4 THEN 'INFORMATION_REQUESTED'
END;

-- (D) Enforce NOT NULL at the DB level (matches @NotNull).
ALTER TABLE trust_onboarding_submission
    ALTER COLUMN status SET NOT NULL;

-- (E) Recreate the partial unique index using the string values.
CREATE UNIQUE INDEX trust_onboarding_submission_partner_id_status_key
    ON trust_onboarding_submission (partner_id)
    WHERE partner_id IS NOT NULL
    AND status IN ('UNSUBMITTED', 'INFORMATION_REQUESTED', 'SUBMITTED');