CREATE TABLE IF NOT EXISTS store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    owner_user_id VARCHAR(64) NOT NULL,
    platform_type VARCHAR(32) NOT NULL,
    platform_shop_id VARCHAR(64) NOT NULL,
    shop_name VARCHAR(128) NOT NULL,
    auth_status VARCHAR(32) NOT NULL DEFAULT 'disconnected',
    profit_threshold DECIMAL(10,2) NOT NULL DEFAULT 0,
    risk_threshold DECIMAL(10,2) NOT NULL DEFAULT 0,
    default_ship_config JSON DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_platform_shop (tenant_id, platform_type, platform_shop_id),
    KEY idx_store_tenant_status (tenant_id, auth_status)
);

CREATE TABLE IF NOT EXISTS channel_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    auth_status VARCHAR(32) NOT NULL DEFAULT 'connected',
    expires_at DATETIME DEFAULT NULL,
    extra_config JSON DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_org_channel_status (organization_id, channel_type, auth_status)
);
