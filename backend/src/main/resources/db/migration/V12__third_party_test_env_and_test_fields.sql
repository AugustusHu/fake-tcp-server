ALTER TABLE channel_config
    ADD COLUMN third_party_test_ip VARCHAR(255) NULL AFTER no_match_response_code,
    ADD COLUMN third_party_test_port INT NULL AFTER third_party_test_ip,
    ADD COLUMN ctmk1 VARCHAR(64) NULL AFTER third_party_test_port,
    ADD COLUMN ctmk2 VARCHAR(64) NULL AFTER ctmk1;

ALTER TABLE channel_key_setting
    ADD COLUMN test_tid VARCHAR(64) NULL AFTER mac_algorithm,
    ADD COLUMN test_pan VARCHAR(64) NULL AFTER test_tid,
    ADD COLUMN test_pin VARCHAR(255) NULL AFTER test_pan,
    ADD COLUMN test_de42 VARCHAR(64) NULL AFTER test_pin,
    ADD COLUMN test_de18 VARCHAR(16) NULL AFTER test_de42,
    ADD COLUMN test_de43 VARCHAR(255) NULL AFTER test_de18,
    ADD COLUMN test_de49 VARCHAR(16) NULL AFTER test_de43;
