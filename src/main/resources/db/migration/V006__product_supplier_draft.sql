CREATE TABLE IF NOT EXISTS candidate_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_url VARCHAR(512) NOT NULL,
    source_url_hash VARCHAR(64) DEFAULT NULL,
    title VARCHAR(256) NOT NULL,
    category VARCHAR(128) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    estimated_profit DECIMAL(10,2) DEFAULT NULL,
    risk_level VARCHAR(16) DEFAULT NULL,
    recommendation_reason VARCHAR(512) DEFAULT NULL,
    ai_summary TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_candidate_store_status_updated (store_id, status, updated_at),
    KEY idx_candidate_store_category_status (store_id, category, status),
    UNIQUE KEY uk_store_source_hash (store_id, source_url_hash)
);

CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    supplier_platform_type VARCHAR(32) NOT NULL,
    supplier_platform_id VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(128) NOT NULL,
    source_url VARCHAR(512) DEFAULT NULL,
    price_score INT DEFAULT NULL,
    delivery_score INT DEFAULT NULL,
    stability_score INT DEFAULT NULL,
    risk_level VARCHAR(16) DEFAULT NULL,
    dropship_support_flag TINYINT(1) NOT NULL DEFAULT 0,
    is_primary TINYINT(1) NOT NULL DEFAULT 0,
    is_backup TINYINT(1) NOT NULL DEFAULT 0,
    blacklist_flag TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_supplier_platform (store_id, supplier_platform_type, supplier_platform_id),
    KEY idx_supplier_store_platform_risk (store_id, supplier_platform_type, risk_level),
    KEY idx_supplier_store_primary (store_id, is_primary)
);

CREATE TABLE IF NOT EXISTS product_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    candidate_product_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    selling_points TEXT DEFAULT NULL,
    detail_content TEXT DEFAULT NULL,
    faq_content TEXT DEFAULT NULL,
    suggested_price DECIMAL(10,2) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'drafting',
    ai_version VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_draft_store_status_updated (store_id, status, updated_at),
    KEY idx_draft_store_candidate (store_id, candidate_product_id)
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    platform_product_id VARCHAR(64) NOT NULL,
    product_draft_id BIGINT DEFAULT NULL,
    title VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'offline',
    health_score INT DEFAULT NULL,
    published_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_platform_product (store_id, platform_product_id),
    KEY idx_product_store_status_updated (store_id, status, updated_at)
);
