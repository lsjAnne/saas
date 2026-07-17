CREATE TABLE IF NOT EXISTS tenant_data_export_task (
    export_task_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    scope_code VARCHAR(64) NOT NULL,
    scope_name VARCHAR(64) NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    file_name VARCHAR(128) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    export_content TEXT NOT NULL,
    masking_strategy VARCHAR(64) DEFAULT NULL,
    time_range_start TIMESTAMP DEFAULT NULL,
    time_range_end TIMESTAMP DEFAULT NULL,
    download_expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP DEFAULT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenant_data_export_task_tenant_created
    ON tenant_data_export_task (tenant_id, created_at, export_task_id);

CREATE TABLE IF NOT EXISTS tenant_compliance_acceptance (
    acceptance_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    document_code VARCHAR(64) NOT NULL,
    document_version VARCHAR(32) NOT NULL,
    accepted_by VARCHAR(64) NOT NULL,
    accepted_source VARCHAR(32) NOT NULL,
    accepted_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenant_compliance_acceptance_lookup
    ON tenant_compliance_acceptance (tenant_id, document_code, accepted_at, acceptance_id);

