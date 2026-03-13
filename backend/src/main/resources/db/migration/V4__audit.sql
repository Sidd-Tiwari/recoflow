-- V4__audit.sql

CREATE TABLE audit_logs (
    audit_id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        NOT NULL REFERENCES tenants(tenant_id),
    actor_user_id UUID       REFERENCES users(user_id),
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    VARCHAR(255),
    before_json  JSONB,
    after_json   JSONB,
    ip_address   VARCHAR(45),
    ts           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_tenant     ON audit_logs(tenant_id);
CREATE INDEX idx_audit_entity     ON audit_logs(tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_actor      ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_ts         ON audit_logs(tenant_id, ts DESC);
