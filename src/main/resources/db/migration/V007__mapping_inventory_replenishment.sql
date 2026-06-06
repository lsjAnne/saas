CREATE TABLE IF NOT EXISTS product_source_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    mapping_type VARCHAR(32) NOT NULL DEFAULT 'primary',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    risk_flag TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_mapping_store_product_active (store_id, product_id, is_active),
    KEY idx_mapping_store_supplier_active (store_id, supplier_id, is_active),
    UNIQUE KEY uk_mapping_store_product_supplier_type (store_id, product_id, supplier_id, mapping_type)
);

CREATE TABLE IF NOT EXISTS inventory_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    available_stock INT NOT NULL DEFAULT 0,
    reserved_stock INT NOT NULL DEFAULT 0,
    safety_stock INT NOT NULL DEFAULT 0,
    snapshot_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_inventory_store_product_sku_snapshot (store_id, product_id, sku_id, snapshot_at),
    KEY idx_inventory_store_safety (store_id, safety_stock, snapshot_at)
);

CREATE TABLE IF NOT EXISTS replenishment_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    suggested_qty INT NOT NULL DEFAULT 0,
    task_status VARCHAR(32) NOT NULL DEFAULT 'draft',
    approval_status VARCHAR(32) NOT NULL DEFAULT 'not_required',
    reason_text VARCHAR(512) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_replenishment_store_task_status (store_id, task_status, approval_status),
    KEY idx_replenishment_product_sku_status (product_id, sku_id, task_status)
);
