CREATE TABLE IF NOT EXISTS plugin_app (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    app_name VARCHAR(128) NOT NULL,
    app_type VARCHAR(64) NOT NULL,
    permission_scope TEXT NOT NULL,
    access_key VARCHAR(64) NOT NULL,
    secret_masked VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_plugin_app_org_name
    ON plugin_app (organization_id, app_name);

CREATE UNIQUE INDEX IF NOT EXISTS uk_plugin_app_access_key
    ON plugin_app (access_key);

CREATE TABLE IF NOT EXISTS webhook_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    event_code VARCHAR(64) NOT NULL,
    callback_url VARCHAR(255) NOT NULL,
    secret_token VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'enabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_webhook_org_event_callback
    ON webhook_subscription (organization_id, event_code, callback_url);

CREATE INDEX IF NOT EXISTS idx_webhook_org_status
    ON webhook_subscription (organization_id, status, id);
