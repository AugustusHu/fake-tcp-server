ALTER TABLE app_user
    ADD COLUMN last_channel_id VARCHAR(100) NULL AFTER display_name;
