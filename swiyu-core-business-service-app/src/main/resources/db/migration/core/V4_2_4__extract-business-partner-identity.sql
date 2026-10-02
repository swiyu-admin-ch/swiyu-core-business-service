-- EID-7099: BusinessPartnerIdentity moves from embedded bpi_* columns on business_entity into its own
-- table. Identity updates (TMS BPI events) then no longer bump business_entity.version and cannot
-- collide with concurrent partner updates (OptimisticLockException on trust onboarding approval).
-- The identity is keyed by the partner id (business_partner_id), as TMS uses the partner id as
-- BusinessPartnerIdentityId.
CREATE TABLE business_partner_identity
(
    business_partner_id UUID PRIMARY KEY,
    valid_until         TIMESTAMPTZ,
    trusted_identifier  JSONB,
    status              VARCHAR(30) NOT NULL,
    last_activated      TIMESTAMPTZ,
    uid                 VARCHAR(255),
    entity_name         JSONB,
    tms_version         BIGINT,
    version             BIGINT NOT NULL,

    CONSTRAINT fk_business_partner_identity__business_partner_id
        FOREIGN KEY (business_partner_id) REFERENCES business_entity (id)
);

INSERT INTO business_partner_identity (business_partner_id, valid_until, trusted_identifier, status,
                                       last_activated, uid, entity_name, tms_version, version)
SELECT id,
       bpi_valid_until,
       bpi_trusted_identifier,
       bpi_status,
       bpi_last_activated,
       bpi_uid,
       bpi_entity_name,
       bpi_tms_version,
       0
FROM business_entity
WHERE bpi_status IS NOT NULL;

-- The bpi_* columns on business_entity are no longer written and will be dropped in a follow-up.
