ALTER TABLE channel_config
    ADD COLUMN mock_tls_enabled BOOLEAN NULL AFTER third_party_tls_enabled,
    ADD COLUMN mock_ctmk1 VARCHAR(64) NULL AFTER mock_tls_enabled,
    ADD COLUMN mock_ctmk2 VARCHAR(64) NULL AFTER mock_ctmk1;
