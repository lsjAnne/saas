CREATE TABLE IF NOT EXISTS member_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    nickname VARCHAR(128) DEFAULT NULL,
    level_code VARCHAR(32) NOT NULL DEFAULT 'normal',
    total_order_count INT NOT NULL DEFAULT 0,
    total_paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    last_order_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_profile_store_customer UNIQUE (store_id, customer_id)
);

CREATE INDEX IF NOT EXISTS idx_member_profile_store_level_created
    ON member_profile (store_id, level_code, created_at);

CREATE INDEX IF NOT EXISTS idx_member_profile_store_last_order
    ON member_profile (store_id, last_order_at);

CREATE TABLE IF NOT EXISTS member_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    member_profile_id BIGINT NOT NULL,
    tag_code VARCHAR(64) NOT NULL,
    tag_name VARCHAR(128) NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'manual',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_tag_member_code UNIQUE (member_profile_id, tag_code)
);

CREATE INDEX IF NOT EXISTS idx_member_tag_store_code
    ON member_tag (store_id, tag_code, created_at);

CREATE TABLE IF NOT EXISTS finance_bill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    bill_type VARCHAR(32) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    income_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    cost_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    gross_profit DECIMAL(10,2) NOT NULL DEFAULT 0,
    bill_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_finance_bill_store_period UNIQUE (store_id, bill_type, period_start, period_end)
);

CREATE INDEX IF NOT EXISTS idx_finance_bill_store_status
    ON finance_bill (store_id, bill_status, created_at);

CREATE TABLE IF NOT EXISTS settlement_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    finance_bill_id BIGINT NOT NULL,
    settlement_type VARCHAR(32) NOT NULL DEFAULT 'bank_transfer',
    settlement_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    settlement_status VARCHAR(32) NOT NULL DEFAULT 'settled',
    settled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_settlement_record_bill_created
    ON settlement_record (finance_bill_id, created_at);
