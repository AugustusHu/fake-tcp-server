ALTER TABLE channel_config
    ADD CONSTRAINT uk_channel_config_name UNIQUE (name);
