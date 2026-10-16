CREATE TABLE payment
(
    id                      UUID          NOT NULL,
    business_partner_id     UUID          NOT NULL,
    reference_id            VARCHAR(16)   NOT NULL,
    status                  VARCHAR(32)   NOT NULL,
    product                 VARCHAR(64)   NOT NULL,
    return_url              VARCHAR(2048) NOT NULL,
    saferpay_token          VARCHAR(255),
    saferpay_expiration     TIMESTAMP WITHOUT TIME ZONE,
    saferpay_redirect_url   VARCHAR(2048),
    saferpay_transaction_id VARCHAR(255),
    inventory_granted_at    TIMESTAMP WITHOUT TIME ZONE,
    payed_at                TIMESTAMP WITHOUT TIME ZONE,
    holdername              VARCHAR(255),
    payment_method          VARCHAR(64),
    amount_value            BIGINT,
    amount_currency         VARCHAR(3),
    version                 BIGINT        NOT NULL,
    last_modified_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_modified_by        VARCHAR(255)  NOT NULL,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by              VARCHAR(255)  NOT NULL,
    CONSTRAINT payment_id_pk PRIMARY KEY (id),
    CONSTRAINT payment_reference_id_uq UNIQUE (reference_id)
);

CREATE INDEX idx_payment__business_partner_id
    ON payment (business_partner_id);

-- At most one open (PENDING) payment per business partner and product.
CREATE UNIQUE INDEX payment_business_partner_id_product_pending_key
    ON payment (business_partner_id, product) WHERE status = 'PENDING';
