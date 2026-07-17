CREATE TABLE IF NOT EXISTS auth_user (
    user_id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    operator_type VARCHAR(32) NOT NULL,
    tenant_id VARCHAR(64) DEFAULT NULL,
    organization_id VARCHAR(64) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    password_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP DEFAULT NULL,
    CONSTRAINT uk_auth_user_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_auth_user_tenant_role
    ON auth_user (tenant_id, role_code, status);

CREATE TABLE IF NOT EXISTS auth_role_permission (
    role_code VARCHAR(32) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_code, permission_code)
);

