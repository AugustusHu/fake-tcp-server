CREATE TABLE mock_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_id VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    match_mode VARCHAR(20) NOT NULL DEFAULT 'ALL',
    conditions_json JSON NOT NULL,
    action_json JSON NOT NULL,
    response_json JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mock_rule_channel_enabled_priority (channel_id, enabled, priority)
);

CREATE TABLE request_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_id VARCHAR(100) NOT NULL,
    remote_address VARCHAR(200),
    mti VARCHAR(10),
    processing_code VARCHAR(20),
    matched_rule_id BIGINT,
    matched_rule_name VARCHAR(200),
    action_type VARCHAR(40),
    response_code VARCHAR(10),
    duration_ms BIGINT,
    request_hex LONGTEXT,
    response_hex LONGTEXT,
    request_fields_json JSON,
    response_fields_json JSON,
    error_message VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_request_log_channel_created (channel_id, created_at),
    INDEX idx_request_log_rule (matched_rule_id)
);
