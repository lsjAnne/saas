CREATE TABLE IF NOT EXISTS tenant_cleanup_task (
    cleanup_task_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    cleanup_scopes_json TEXT DEFAULT NULL,
    requested_by VARCHAR(64) NOT NULL,
    reviewed_by VARCHAR(64) DEFAULT NULL,
    executed_by VARCHAR(64) DEFAULT NULL,
    impact_summary_json TEXT NOT NULL,
    result_summary_json TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    executed_at TIMESTAMP DEFAULT NULL,
    completed_at TIMESTAMP DEFAULT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenant_cleanup_task_tenant_created
    ON tenant_cleanup_task (tenant_id, created_at, cleanup_task_id);

