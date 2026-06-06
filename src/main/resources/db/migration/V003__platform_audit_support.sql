CREATE TABLE IF NOT EXISTS organization_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(64) NOT NULL,
    mobile VARCHAR(32) DEFAULT NULL,
    role_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_org_member_org_id (organization_id)
);

CREATE TABLE IF NOT EXISTS support_session (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    requester_id VARCHAR(64) DEFAULT NULL,
    reason VARCHAR(255) NOT NULL,
    approver VARCHAR(64) DEFAULT NULL,
    expires_at DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    approval_remark VARCHAR(255) DEFAULT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_support_session_tenant_id (tenant_id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operator_type VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_log_tenant_id (tenant_id),
    KEY idx_audit_log_trace_id (trace_id)
);
