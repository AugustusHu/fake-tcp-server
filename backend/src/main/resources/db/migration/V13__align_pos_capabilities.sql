UPDATE mock_rule
SET capability = 'TID_INIT_9G',
    system_conditions_json = JSON_SET(system_conditions_json, '$[1].value', '9G0000')
WHERE capability = 'TID_INIT_9B';

UPDATE mock_rule
SET capability = 'TID_INIT_9B',
    system_conditions_json = JSON_SET(system_conditions_json, '$[1].value', '9B0000')
WHERE capability = 'TID_INIT_9C';

UPDATE mock_rule
SET system_conditions_json = JSON_SET(system_conditions_json, '$[0].value', '0100', '$[1].value', '310000'),
    response_json = JSON_SET(response_json, '$.mti', '0110')
WHERE capability = 'BALANCE_QUERY'
  AND JSON_UNQUOTE(JSON_EXTRACT(system_conditions_json, '$[0].value')) = '0200';

UPDATE mock_rule
SET system_conditions_json = JSON_SET(system_conditions_json, '$[1].value', '600000')
WHERE capability = 'PRE_AUTH'
  AND JSON_UNQUOTE(JSON_EXTRACT(system_conditions_json, '$[1].value')) = '030000';

UPDATE mock_rule
SET system_conditions_json = JSON_SET(system_conditions_json, '$[1].value', '610000')
WHERE capability = 'PRE_AUTH_COMPLETION'
  AND JSON_UNQUOTE(JSON_EXTRACT(system_conditions_json, '$[1].value')) = '000000';

UPDATE mock_rule
SET system_conditions_json = JSON_SET(system_conditions_json, '$[1].value', '9C0000')
WHERE capability = 'PARAMETER_DOWNLOAD'
  AND JSON_UNQUOTE(JSON_EXTRACT(system_conditions_json, '$[1].value')) = '9G0000';

UPDATE mock_rule
SET system_conditions_json = JSON_SET(system_conditions_json, '$[1].value', '9D0000')
WHERE capability = 'CALLHOME'
  AND JSON_UNQUOTE(JSON_EXTRACT(system_conditions_json, '$[1].value')) = '9H0000';
