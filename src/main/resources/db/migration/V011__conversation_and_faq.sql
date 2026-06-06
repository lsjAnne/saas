CREATE TABLE IF NOT EXISTS customer_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    platform_type VARCHAR(32) NOT NULL,
    platform_conversation_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) DEFAULT NULL,
    conversation_status VARCHAR(32) NOT NULL DEFAULT 'open',
    risk_flag TINYINT(1) NOT NULL DEFAULT 0,
    last_message_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_customer_conversation_store_platform UNIQUE (store_id, platform_type, platform_conversation_id)
);

CREATE INDEX IF NOT EXISTS idx_customer_conversation_store_status_updated
    ON customer_conversation (store_id, conversation_status, updated_at);

CREATE TABLE IF NOT EXISTS conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_type VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL DEFAULT 'text',
    content_text VARCHAR(4000) NOT NULL,
    ai_generated_flag TINYINT(1) NOT NULL DEFAULT 0,
    risk_flag TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conversation_message_conversation_created
    ON conversation_message (conversation_id, created_at);

CREATE TABLE IF NOT EXISTS faq_knowledge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    product_id BIGINT DEFAULT NULL,
    question VARCHAR(512) NOT NULL,
    answer VARCHAR(4000) NOT NULL,
    source_type VARCHAR(32) DEFAULT NULL,
    is_enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_faq_knowledge_store_product_enabled
    ON faq_knowledge (store_id, product_id, is_enabled);
