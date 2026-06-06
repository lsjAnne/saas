CREATE TABLE IF NOT EXISTS automation_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    rule_category VARCHAR(32) NOT NULL DEFAULT 'general',
    risk_category VARCHAR(32) NOT NULL DEFAULT 'general',
    rule_name VARCHAR(128) NOT NULL,
    rule_expression TEXT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_automation_rule_store_type
    ON automation_rule (store_id, rule_type, id);

CREATE INDEX IF NOT EXISTS idx_automation_rule_store_enabled
    ON automation_rule (store_id, is_enabled, id);
