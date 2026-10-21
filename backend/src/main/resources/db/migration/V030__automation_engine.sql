-- 运营自动化中心 M1：自动化执行引擎（达梦兼容：TEXT/VARCHAR 替代 JSON，主键 VARCHAR(36) 存 UUID）

-- 规则动作绑定表：一条规则可绑定多个动作，动作按 priority 升序依次执行
CREATE TABLE IF NOT EXISTS automation_action (
    action_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(36),
    store_id VARCHAR(36),
    rule_id VARCHAR(36),
    action_type VARCHAR(32),
    action_config TEXT,
    priority INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_automation_action_store
    ON automation_action (store_id, enabled, priority);

CREATE INDEX IF NOT EXISTS idx_automation_action_rule
    ON automation_action (rule_id);

-- 自动化执行记录表：每次动作执行写一行，可审计、可追溯、幂等去重
CREATE TABLE IF NOT EXISTS automation_execution (
    execution_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(36),
    store_id VARCHAR(36),
    rule_id VARCHAR(36),
    action_type VARCHAR(32),
    trigger_source VARCHAR(16),
    target_type VARCHAR(32),
    target_id VARCHAR(64),
    result TEXT,
    status VARCHAR(20),
    retry_count INT DEFAULT 0,
    cost_ms BIGINT DEFAULT 0,
    error_message TEXT,
    executed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_automation_execution_store
    ON automation_execution (store_id, status, executed_at);

CREATE INDEX IF NOT EXISTS idx_automation_execution_target
    ON automation_execution (tenant_id, target_id, rule_id, action_type);

-- 人工确认队列表：PAUSE_REVIEW 动作挂起后进入人工队列，APPROVED 放行后续动作，REJECTED 终止
CREATE TABLE IF NOT EXISTS automation_approval (
    task_id VARCHAR(36) NOT NULL PRIMARY KEY,
    execution_id VARCHAR(36),
    tenant_id VARCHAR(36),
    store_id VARCHAR(36),
    target_type VARCHAR(32),
    target_id VARCHAR(64),
    reason TEXT,
    status VARCHAR(20),
    reviewed_by VARCHAR(64),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_automation_approval_store_status
    ON automation_approval (store_id, status, created_at);
