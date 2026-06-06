CREATE TABLE IF NOT EXISTS exception_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    related_type VARCHAR(32) NOT NULL,
    related_id VARCHAR(64) NOT NULL,
    exception_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'P2',
    status VARCHAR(32) NOT NULL DEFAULT 'new',
    suggestion_text VARCHAR(512) DEFAULT NULL,
    owner_user_id VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exception_task_store_status_severity
    ON exception_task (store_id, status, severity, created_at);

CREATE INDEX IF NOT EXISTS idx_exception_task_related_status
    ON exception_task (related_type, related_id, status);

CREATE INDEX IF NOT EXISTS idx_exception_task_type_created
    ON exception_task (exception_type, created_at);
