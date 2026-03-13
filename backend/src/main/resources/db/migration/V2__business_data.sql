-- V2__business_data.sql

CREATE TABLE customers (
    customer_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(tenant_id),
    name        VARCHAR(255) NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(255),
    gstin       VARCHAR(15),
    vpa_hint    VARCHAR(255),
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE invoices (
    invoice_id   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID           NOT NULL REFERENCES tenants(tenant_id),
    customer_id  UUID           NOT NULL REFERENCES customers(customer_id),
    invoice_no   VARCHAR(50)    NOT NULL,
    invoice_date DATE           NOT NULL,
    due_date     DATE,
    subtotal     NUMERIC(12,2)  NOT NULL DEFAULT 0,
    tax_amount   NUMERIC(12,2)  NOT NULL DEFAULT 0,
    total        NUMERIC(12,2)  NOT NULL DEFAULT 0,
    paid_amount  NUMERIC(12,2)  NOT NULL DEFAULT 0,
    status       VARCHAR(50)    NOT NULL DEFAULT 'DRAFT',
    notes        TEXT,
    created_by   UUID           REFERENCES users(user_id),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_invoice_no_tenant UNIQUE (tenant_id, invoice_no)
);

CREATE TABLE invoice_items (
    item_id     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id  UUID          NOT NULL REFERENCES invoices(invoice_id) ON DELETE CASCADE,
    description VARCHAR(500)  NOT NULL,
    quantity    NUMERIC(10,3) NOT NULL DEFAULT 1,
    rate        NUMERIC(12,2) NOT NULL,
    tax_pct     NUMERIC(5,2)  NOT NULL DEFAULT 0,
    amount      NUMERIC(12,2) NOT NULL,
    sort_order  INT           NOT NULL DEFAULT 0
);

CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_invoices_tenant  ON invoices(tenant_id);
CREATE INDEX idx_invoices_customer ON invoices(customer_id);
CREATE INDEX idx_invoices_status  ON invoices(tenant_id, status);
CREATE INDEX idx_invoices_date    ON invoices(tenant_id, invoice_date);
CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
