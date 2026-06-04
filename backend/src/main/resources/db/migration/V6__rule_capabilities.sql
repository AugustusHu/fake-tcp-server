ALTER TABLE mock_rule
    ADD COLUMN capability VARCHAR(60) NOT NULL DEFAULT 'DEBIT' AFTER public_rule,
    ADD COLUMN system_conditions_json JSON NULL AFTER match_mode;

UPDATE mock_rule
SET system_conditions_json = '[{"field":"0","operator":"EQ","value":"0200"},{"field":"3","operator":"EQ","value":"000000"}]'
WHERE system_conditions_json IS NULL;

CREATE INDEX idx_mock_rule_channel_capability ON mock_rule(channel_id, capability);
