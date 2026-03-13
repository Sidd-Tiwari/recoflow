-- V3__import_matching.sql

CREATE TABLE statement_files (
    file_id      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        NOT NULL REFERENCES tenants(tenant_id),
    filename     VARCHAR(255) NOT NULL,
    uploaded_by  UUID        REFERENCES users(user_id),
    status       VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    total_rows   INT,
    valid_rows   INT,
    invalid_rows INT,
    error_detail TEXT,
    parsed_at    TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE transactions (
    txn_id     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID          NOT NULL REFERENCES tenants(tenant_id),
    file_id    UUID          NOT NULL REFERENCES statement_files(file_id),
    txn_time   TIMESTAMPTZ   NOT NULL,
    amount     NUMERIC(12,2) NOT NULL,
    utr        VARCHAR(50),
    remark     TEXT,
    payer_vpa  VARCHAR(255),
    txn_type   VARCHAR(20)   NOT NULL DEFAULT 'CREDIT',
    raw_json   JSONB,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_utr_tenant UNIQUE (tenant_id, utr)
);

CREATE TABLE reconciliations (
    recon_id       UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID          NOT NULL REFERENCES tenants(tenant_id),
    txn_id         UUID          NOT NULL REFERENCES transactions(txn_id),
    invoice_id     UUID          NOT NULL REFERENCES invoices(invoice_id),
    matched_amount NUMERIC(12,2) NOT NULL,
    confidence     NUMERIC(5,4)  NOT NULL DEFAULT 0,
    reason         JSONB,
    status         VARCHAR(50)   NOT NULL DEFAULT 'SUGGESTED',
    confirmed_by   UUID          REFERENCES users(user_id),
    confirmed_at   TIMESTAMPTZ,
    notes          TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_statement_files_tenant ON statement_files(tenant_id);
CREATE INDEX idx_transactions_tenant    ON transactions(tenant_id);
CREATE INDEX idx_transactions_file      ON transactions(file_id);
CREATE INDEX idx_transactions_txn_time  ON transactions(tenant_id, txn_time);
CREATE INDEX idx_reconciliations_tenant ON reconciliations(tenant_id);
CREATE INDEX idx_reconciliations_txn    ON reconciliations(txn_id);
CREATE INDEX idx_reconciliations_invoice ON reconciliations(invoice_id);
CREATE INDEX idx_reconciliations_status ON reconciliations(tenant_id, status);
