-- Migrate legacy business entities with UNKNOWN or INDIVIDUAL type to BUSINESS.
-- UNKNOWN was a temporary placeholder for partners onboarded before the type concept was introduced.
-- INDIVIDUAL was used by the old v1 onboarding flow; the new v2 flow always requires an explicit type,
-- and individual persons are not supported as business partners going forward.
UPDATE business_entity
SET type = 'BUSINESS'
WHERE type IN ('UNKNOWN', 'INDIVIDUAL');
