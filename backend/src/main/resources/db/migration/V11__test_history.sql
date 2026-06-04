CREATE TABLE test_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    channel_id VARCHAR(80) NOT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    matched BOOLEAN NOT NULL DEFAULT FALSE,
    rule_id BIGINT NULL,
    rule_name VARCHAR(255) NULL,
    request_xml LONGTEXT NOT NULL,
    response_xml LONGTEXT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_history_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_test_history_scope_created (user_id, channel_id, created_at)
);
