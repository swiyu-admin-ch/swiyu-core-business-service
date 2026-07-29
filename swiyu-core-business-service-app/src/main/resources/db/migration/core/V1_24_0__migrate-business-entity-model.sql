-- ============================================================
-- EID-6616: Migrate BusinessEntity model
-- - Add Contact embed columns (correspondingLanguage only; email+phone already exist)
-- - Add BusinessPartnerIdentity columns
-- - Seed BusinessPartnerIdentity from latest SUCCEEDED TrustOnboardingSubmission
--   + any subsequently SUCCEEDED TrustAdditionalDidsSubmission
-- - Drop trust_verification_status and max_date_for_trust_verification_status
--
-- NOTE: There is no aggregated_trust_status column. The trust verification status
-- is computed on every read in BusinessPartnerService.computeVerificationProgress()
-- from the BusinessPartnerIdentity + TrustOnboardingSubmission history.
-- ============================================================

-- 1. Add contact columns to business_entity
--    contact_email and contact_phone already exist (added in V1_13_8).
--    contact_first_name and contact_last_name are always NULL on BusinessEntity
--    (those fields belong to TrustOnboardingSubmission.contactPerson), but the
--    shared Contact embeddable requires the columns to be present for Hibernate
--    schema validation.
ALTER TABLE business_entity
    ADD COLUMN contact_corresponding_language VARCHAR(10),
    ADD COLUMN contact_first_name             VARCHAR(255),
    ADD COLUMN contact_last_name              VARCHAR(255);

-- Add contact_corresponding_language to trust_onboarding_submission.
-- The Contact embeddable gained this field; TrustOnboardingSubmission.contactPerson
-- maps it as insertable=false/updatable=false (the value lives in the root-level
-- corresponding_language column until EID-6618 unifies them).
ALTER TABLE trust_onboarding_submission
    ADD COLUMN contact_corresponding_language VARCHAR(10);

-- 2. Add BusinessPartnerIdentity columns
ALTER TABLE business_entity
    ADD COLUMN bpi_valid_until          TIMESTAMPTZ,
    ADD COLUMN bpi_trusted_identifier   JSONB,
    ADD COLUMN bpi_status               VARCHAR(30),
    ADD COLUMN bpi_last_activated       TIMESTAMPTZ,
    ADD COLUMN bpi_uid                  VARCHAR(255),
    ADD COLUMN bpi_entity_name          JSONB,
    ADD COLUMN bpi_tms_version          BIGINT;

-- ============================================================
-- 3. Seed BusinessPartnerIdentity
--
-- Strategy (temporary until TMS publishes BPI sync events via EID-6612):
--   a) Base data (uid, entityName, lastActivated) comes from the latest
--      SUCCEEDED TrustOnboardingSubmission.
--   b) trustedIdentifier is the union of:
--        - all VALID DIDs from that TOS's proofOfPossessions JSONB array
--        - all VALID DIDs from every SUCCEEDED TrustAdditionalDidsSubmission
--          for the same partner
--   c) bpi_tms_version is left NULL (no TMS version available yet).
-- ============================================================

-- 3a. Seed base data from latest SUCCEEDED TrustOnboardingSubmission
UPDATE business_entity be
SET bpi_status        = 'ACTIVE',
    bpi_last_activated = tos.submitted_at,
    bpi_uid            = tos.uid,
    bpi_entity_name    = tos.entity_name
FROM (
    SELECT DISTINCT ON (partner_id)
        partner_id,
        submitted_at,
        uid,
        entity_name
    FROM trust_onboarding_submission
    WHERE status = 'SUCCEEDED'
    ORDER BY partner_id, submitted_at DESC
) tos
WHERE be.id = tos.partner_id;

-- 3b. Build the trustedIdentifier list for each partner that has a SUCCEEDED submission:
--
--   Step i:  Extract VALID DIDs from the latest SUCCEEDED TrustOnboardingSubmission.
--            proofOfPossessions is a JSONB array of objects: [{did, nonce, status, ...}, ...]
--            We keep only entries where status = 'VALID'.
--
--   Step ii: Union in VALID DIDs from every SUCCEEDED TrustAdditionalDidsSubmission
--            for the same partner (didsToAdd is also a JSONB array of ProofOfPossession objects).
--
--   Step iii: Deduplicate and write back as a plain JSONB array of DID strings.

WITH tos_dids AS (
    -- Latest SUCCEEDED TOS per partner: extract VALID did values from proofOfPossessions
    SELECT DISTINCT ON (partner_id)
        partner_id,
        (
            SELECT jsonb_agg(pop->>'did')
            FROM jsonb_array_elements(
                COALESCE(proof_of_possessions, '[]'::jsonb)
            ) AS pop
            WHERE pop->>'status' = 'VALID'
              AND pop->>'did' IS NOT NULL
        ) AS tos_valid_dids
    FROM trust_onboarding_submission
    WHERE status = 'SUCCEEDED'
    ORDER BY partner_id, submitted_at DESC
),
add_dids AS (
    -- All SUCCEEDED TrustAdditionalDidsSubmissions per partner: aggregate VALID dids_to_add DIDs
    SELECT
        partner_id,
        jsonb_agg(DISTINCT pop->>'did') AS additional_dids
    FROM trust_additional_dids_submission,
         jsonb_array_elements(dids_to_add) AS pop
    WHERE status = 'SUCCEEDED'
      AND pop->>'status' = 'VALID'
      AND pop->>'did' IS NOT NULL
    GROUP BY partner_id
),
combined AS (
    SELECT
        t.partner_id,
        -- Union TOS DIDs and additional DIDs, remove nulls and duplicates
        (
            SELECT jsonb_agg(DISTINCT did_value)
            FROM (
                SELECT jsonb_array_elements_text(COALESCE(t.tos_valid_dids, '[]'::jsonb)) AS did_value
                UNION
                SELECT jsonb_array_elements_text(COALESCE(a.additional_dids, '[]'::jsonb)) AS did_value
            ) all_dids
            WHERE did_value IS NOT NULL
        ) AS all_trusted_identifiers
    FROM tos_dids t
    LEFT JOIN add_dids a ON a.partner_id = t.partner_id
)
UPDATE business_entity be
SET bpi_trusted_identifier = combined.all_trusted_identifiers
FROM combined
WHERE be.id = combined.partner_id
  AND be.bpi_status = 'ACTIVE';

-- 4. Drop the columns that are now replaced by BusinessPartnerIdentity
ALTER TABLE business_entity
    DROP COLUMN trust_verification_status,
    DROP COLUMN max_date_for_trust_verification_status;
