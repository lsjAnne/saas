CREATE TABLE IF NOT EXISTS live_product_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    live_plan_id BIGINT NOT NULL,
    candidate_product_id BIGINT DEFAULT NULL,
    product_draft_id BIGINT DEFAULT NULL,
    product_id BIGINT DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_product_item_plan_sort
    ON live_product_item (live_plan_id, sort_order, id);

CREATE INDEX IF NOT EXISTS idx_live_product_item_source
    ON live_product_item (product_id, product_draft_id, candidate_product_id);
