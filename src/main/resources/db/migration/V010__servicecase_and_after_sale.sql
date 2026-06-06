CREATE TABLE IF NOT EXISTS customer_service_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    customer_id VARCHAR(64) DEFAULT NULL,
    ticket_status VARCHAR(32) NOT NULL DEFAULT 'open',
    risk_flag TINYINT(1) NOT NULL DEFAULT 0,
    ai_reply_suggestion VARCHAR(4000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customer_service_ticket_store_status
    ON customer_service_ticket (store_id, ticket_status, created_at);

CREATE INDEX IF NOT EXISTS idx_customer_service_ticket_order_status
    ON customer_service_ticket (order_id, ticket_status);

CREATE TABLE IF NOT EXISTS after_sale_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    after_sale_type VARCHAR(32) NOT NULL,
    reason_text VARCHAR(512) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'created',
    evidence_blob VARCHAR(4000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_after_sale_record_order_status
    ON after_sale_record (order_id, status);

CREATE INDEX IF NOT EXISTS idx_after_sale_record_type
    ON after_sale_record (after_sale_type);
