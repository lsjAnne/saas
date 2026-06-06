CREATE TABLE IF NOT EXISTS integration_credential (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plugin_app_id BIGINT NOT NULL,
    credential_type VARCHAR(32) NOT NULL,
    access_key VARCHAR(64) NOT NULL,
    secret_digest VARCHAR(128) NOT NULL,
    secret_key_mask VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_integration_credential_app_type
    ON integration_credential (plugin_app_id, credential_type);

CREATE UNIQUE INDEX IF NOT EXISTS uk_integration_credential_access_key
    ON integration_credential (access_key);

CREATE INDEX IF NOT EXISTS idx_integration_credential_expire
    ON integration_credential (expires_at, id);
