ALTER TABLE channel_config
    ADD COLUMN packager_config_mode VARCHAR(40) NOT NULL DEFAULT 'CLASS_NAME' AFTER packager_type,
    ADD COLUMN packager_file_name VARCHAR(1000) AFTER packager_location,
    ADD COLUMN packager_content LONGTEXT AFTER packager_file_name;
