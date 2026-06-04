ALTER TABLE channel_config
    ADD COLUMN third_party_tls_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER third_party_test_port;
