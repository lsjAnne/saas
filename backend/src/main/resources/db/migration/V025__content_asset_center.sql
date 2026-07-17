CREATE TABLE IF NOT EXISTS content_asset (
    asset_id VARCHAR(64) PRIMARY KEY,
    store_id VARCHAR(64) NOT NULL,
    asset_category VARCHAR(32) NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    asset_status VARCHAR(32) NOT NULL,
    preview_mode VARCHAR(32) NOT NULL,
    preview_url VARCHAR(512) DEFAULT NULL,
    preview_text TEXT DEFAULT NULL,
    generated_flag BOOLEAN NOT NULL DEFAULT FALSE,
    source_channel VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_content_asset_store_category_status
    ON content_asset (store_id, asset_category, asset_status, updated_at);

CREATE TABLE IF NOT EXISTS content_asset_version (
    version_id VARCHAR(64) PRIMARY KEY,
    asset_id VARCHAR(64) NOT NULL,
    version_no INT NOT NULL,
    version_label VARCHAR(32) NOT NULL,
    version_status VARCHAR(32) NOT NULL,
    change_summary VARCHAR(255) DEFAULT NULL,
    content_snapshot TEXT DEFAULT NULL,
    preview_url VARCHAR(512) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_content_asset_version_asset_no
    ON content_asset_version (asset_id, version_no, created_at);

CREATE TABLE IF NOT EXISTS content_asset_reference (
    reference_id VARCHAR(64) PRIMARY KEY,
    asset_id VARCHAR(64) NOT NULL,
    reference_type VARCHAR(32) NOT NULL,
    reference_name VARCHAR(255) NOT NULL,
    reference_target_id VARCHAR(128) NOT NULL,
    quote_text VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_content_asset_reference_asset_created
    ON content_asset_reference (asset_id, created_at, reference_id);
