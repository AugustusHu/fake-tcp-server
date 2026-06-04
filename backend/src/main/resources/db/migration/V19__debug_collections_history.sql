CREATE TABLE debug_collection (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    environment_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    requests_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_debug_collection_user_env (user_id, environment_id),
    CONSTRAINT fk_debug_collection_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_debug_collection_environment FOREIGN KEY (environment_id) REFERENCES debug_environment(id) ON DELETE CASCADE
);

CREATE TABLE debug_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    environment_id VARCHAR(64) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    capability VARCHAR(64) NULL,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    response_code VARCHAR(80) NULL,
    request_xml LONGTEXT NULL,
    result_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_debug_history_user_env (user_id, environment_id, created_at),
    CONSTRAINT fk_debug_history_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_debug_history_environment FOREIGN KEY (environment_id) REFERENCES debug_environment(id) ON DELETE CASCADE
);
