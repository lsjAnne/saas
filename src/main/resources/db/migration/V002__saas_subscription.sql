CREATE TABLE IF NOT EXISTS subscription_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(64) NOT NULL,
    plan_name VARCHAR(128) NOT NULL,
    billing_type VARCHAR(32) NOT NULL DEFAULT 'fixed',
    monthly_price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    yearly_price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    seat_limit INT DEFAULT NULL,
    feature_flags JSON DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'enabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_code (plan_code)
);

CREATE TABLE IF NOT EXISTS tenant_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    subscription_status VARCHAR(32) NOT NULL DEFAULT 'trialing',
    started_at DATETIME NOT NULL,
    expired_at DATETIME DEFAULT NULL,
    seat_count INT NOT NULL DEFAULT 1,
    auto_renew_flag TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tenant_status_expired (tenant_id, subscription_status, expired_at)
);

CREATE TABLE IF NOT EXISTS usage_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    quota_code VARCHAR(64) NOT NULL,
    quota_limit INT NOT NULL DEFAULT 0,
    used_amount INT NOT NULL DEFAULT 0,
    reset_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_quota_code (tenant_id, quota_code)
);
