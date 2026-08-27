-- ============================================================
-- EID-6618: Unify trust onboarding correspondence language.
-- The correspondence language moves from the root-level
-- trust_onboarding_submission.corresponding_language column
-- into the embedded contact column contact_corresponding_language
-- (added read-only in V1_24_0 / EID-6616).
--
-- The root column stores the Language enum as an ORDINAL (SMALLINT);
-- the contact column stores it as the enum NAME (VARCHAR), mapped
-- with @Enumerated(EnumType.STRING). Enum order (see Language.java):
--   0=EN, 1=DE, 2=FR, 3=IT, 4=RM
--
-- This is the "expand" half of an expand/contract migration: the data is
-- copied into the new column and the application stops reading/writing the
-- old one. The now-obsolete, nullable corresponding_language column is left
-- in place on purpose so a rollback to the previous app version still has
-- the data. Dropping it is deferred to a follow-up contract story.
-- ============================================================

-- Copy the ordinal value into the string-based contact column,
-- only where the contact value has not already been set.
UPDATE trust_onboarding_submission
SET contact_corresponding_language = CASE corresponding_language
        WHEN 0 THEN 'EN'
        WHEN 1 THEN 'DE'
        WHEN 2 THEN 'FR'
        WHEN 3 THEN 'IT'
        WHEN 4 THEN 'RM'
        ELSE NULL
    END
WHERE contact_corresponding_language IS NULL
  AND corresponding_language IS NOT NULL;
