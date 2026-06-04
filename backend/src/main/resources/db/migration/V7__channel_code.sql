ALTER TABLE channel_config
    ADD COLUMN channel_code VARCHAR(100) NULL AFTER id;

UPDATE channel_config
SET channel_code = id
WHERE channel_code IS NULL OR TRIM(channel_code) = '';

ALTER TABLE channel_config
    MODIFY channel_code VARCHAR(100) NOT NULL;

ALTER TABLE channel_config
    DROP INDEX uk_channel_config_name;

ALTER TABLE channel_config
    ADD CONSTRAINT uk_channel_config_code UNIQUE (channel_code);
