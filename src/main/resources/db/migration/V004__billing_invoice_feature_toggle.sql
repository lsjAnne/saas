ALTER TABLE tenant
    ADD COLUMN IF NOT EXISTS feature_flags JSON DEFAULT NULL;

CREATE TABLE IF NOT EXISTS billing_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT DEFAULT NULL,
    order_type VARCHAR(32) NOT NULL,
    payable_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(32) NOT NULL DEFAULT 'paid',
    external_order_no VARCHAR(128) DEFAULT NULL,
    paid_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_payment_status_created (tenant_id, payment_status, created_at)
);

CREATE TABLE IF NOT EXISTS invoice_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    billing_order_id BIGINT NOT NULL,
    invoice_title VARCHAR(256) NOT NULL,
    invoice_tax_no VARCHAR(64) DEFAULT NULL,
    invoice_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_invoice_status_created (tenant_id, invoice_status, created_at)
);
