ALTER TABLE business_entity
    ADD COLUMN trust_verification_status varchar(30) NOT NULL DEFAULT 'NOT_VERIFIED';

-- Update trust_verification_status according to status of TrustOnboardingSubmissions
WITH tos AS (
    SELECT DISTINCT ON (partner_id)
    partner_id,
    status
FROM trust_onboarding_submission
ORDER BY partner_id, last_modified_at DESC
    )
UPDATE business_entity AS bp
SET trust_verification_status =
        CASE tos.status
            WHEN 'UNSUBMITTED'           THEN 'VERIFICATION_STARTED'
            WHEN 'SUBMITTED'             THEN 'VERIFICATION_IN_PROGRESS'
            WHEN 'SUCCEEDED'             THEN 'VERIFIED'
            WHEN 'INFORMATION_REQUESTED' THEN 'INFORMATION_REQUESTED'
            ELSE 'NOT_VERIFIED' -- e.g. UNSUBMITTED_TIMEOUT, REJECTED
            END
    FROM tos
WHERE bp.id = tos.partner_id;

ALTER TABLE business_entity
    ADD COLUMN max_date_for_trust_verification_status TIMESTAMP;

