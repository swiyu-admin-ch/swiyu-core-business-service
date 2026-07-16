-- Re-apply migration from V1_21_0: migrate any remaining business_entity records with UNKNOWN or INDIVIDUAL type to BUSINESS.
-- This covers partners that may have been created between the V1_21_0 migration and the enforcement of the type constraint.
UPDATE business_entity
SET type = 'BUSINESS'
WHERE type IN ('UNKNOWN', 'INDIVIDUAL');

-- Sync requested_partner_type on TrustOnboardingSubmission with the current type of the associated business_entity.
-- This ensures submissions that were created with UNKNOWN or INDIVIDUAL reflect the migrated BUSINESS type.
UPDATE trust_onboarding_submission tos
SET requested_partner_type = be.type
FROM business_entity be
WHERE tos.partner_id = be.id
  AND tos.status = 'UNSUBMITTED'
  AND tos.requested_partner_type IS DISTINCT FROM be.type;
