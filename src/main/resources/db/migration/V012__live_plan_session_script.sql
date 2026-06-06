CREATE TABLE IF NOT EXISTS live_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    live_account_id BIGINT DEFAULT NULL,
    plan_name VARCHAR(128) NOT NULL,
    plan_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    scheduled_start_at DATETIME DEFAULT NULL,
    scheduled_end_at DATETIME DEFAULT NULL,
    anchor_profile_name VARCHAR(128) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_plan_store_status_time
    ON live_plan (store_id, plan_status, scheduled_start_at);

CREATE INDEX IF NOT EXISTS idx_live_plan_live_account
    ON live_plan (live_account_id);

CREATE TABLE IF NOT EXISTS live_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    live_plan_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    live_account_id BIGINT DEFAULT NULL,
    session_status VARCHAR(32) NOT NULL DEFAULT 'created',
    room_id VARCHAR(64) DEFAULT NULL,
    actual_start_at DATETIME DEFAULT NULL,
    actual_end_at DATETIME DEFAULT NULL,
    error_message VARCHAR(512) DEFAULT NULL,
    control_mode VARCHAR(32) NOT NULL DEFAULT 'standard',
    current_scene VARCHAR(64) NOT NULL DEFAULT 'default_scene',
    takeover_status VARCHAR(32) NOT NULL DEFAULT 'auto',
    takeover_operator VARCHAR(64) DEFAULT NULL,
    promise_audit_status VARCHAR(32) NOT NULL DEFAULT 'not_reviewed',
    promise_audit_remark VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_session_plan_status
    ON live_session (live_plan_id, session_status);

CREATE INDEX IF NOT EXISTS idx_live_session_account_status
    ON live_session (live_account_id, session_status);

CREATE INDEX IF NOT EXISTS idx_live_session_tenant_store_status
    ON live_session (tenant_id, store_id, session_status);

CREATE TABLE IF NOT EXISTS live_script (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    live_plan_id BIGINT NOT NULL,
    product_id BIGINT DEFAULT NULL,
    script_version VARCHAR(64) DEFAULT NULL,
    script_content TEXT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_script_plan_active
    ON live_script (live_plan_id, is_active);
