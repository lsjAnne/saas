CREATE TABLE IF NOT EXISTS approval_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    approval_type VARCHAR(64) NOT NULL,
    related_type VARCHAR(64) NOT NULL,
    related_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_handler_id VARCHAR(64) DEFAULT NULL,
    remark TEXT DEFAULT NULL,
    result_remark TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_instance_tenant_status
    ON approval_instance (tenant_id, status, id);

CREATE INDEX IF NOT EXISTS idx_approval_instance_related
    ON approval_instance (tenant_id, related_type, related_id, status, id);
