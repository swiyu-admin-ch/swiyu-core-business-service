ALTER TABLE trust_onboarding_submission
    ADD declaration_of_intent_document_id UUID NULL,
    ADD declaration_of_intent_validation_report JSONB NULL;

UPDATE trust_onboarding_submission
SET declaration_of_intent_document_id = (declaration_of_intent ->> 'fullySignedDocumentId')::UUID,
    declaration_of_intent_validation_report = declaration_of_intent -> 'validationReport'
WHERE declaration_of_intent IS NOT NULL;

ALTER TABLE trust_onboarding_submission DROP COLUMN declaration_of_intent;

ALTER TABLE trust_onboarding_submission
    ADD CONSTRAINT fk_trust_onboarding_submission__doi_document_id
        FOREIGN KEY (declaration_of_intent_document_id) REFERENCES partner_document (id)
            ON DELETE SET NULL;

CREATE INDEX idx_trust_onboarding_submission__doi_document_id
    ON trust_onboarding_submission (declaration_of_intent_document_id);
