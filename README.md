# Fake TCP Server

Stateless ISO8583 fake TCP server for multi-channel integration testing.

## What is included

- Spring Boot 2 backend with MySQL 8 persistence.
- jPOS ISO8583 codec.
- Multi-channel startup configuration.
- Per-channel packager configuration by XML content/file or `ISOBasePackager` class name.
- Per-channel stateless mock rules with priority.
- Runtime rule updates through REST API and Vue UI.
- Rule actions: respond, delayed response, timeout, disconnect.
- POS Debug workbench for real third-party test environments, including XML generation, TID initialization, MAC/PIN handling, response parsing, and console logs.
- Request log with decoded fields and raw hex.
- Docker Compose deployment.

## Runtime boundary

Hot update:

- Mock rules.
- Enable or disable rules.
- Priority and response field values.
- Channel code and third-party test environment fields.
- Key settings, including TPK, TSK, MAC strategy, and test transaction fields.

Restart required:

- Creating or deleting a TCP listener channel.
- TCP listener ports.
- Framing.
- TPDU/Header length and response mode.
- ISO8583 packager XML or class name.

## Start with Docker Compose

```bash
docker compose up --build
```

Open the UI:

```text
http://localhost:5173
```

Default services:

- Backend API: `http://localhost:18080`
- MySQL: `localhost:3306`
- Sample TCP channel: `localhost:14400`

## Production with host Nginx

Use `docker-compose.prod.yml` when the server already has host Nginx. Containers bind only to `127.0.0.1`; host Nginx owns the public ports.

First deployment:

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
```

Routine release after code changes:

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build --no-deps backend frontend
docker compose -f docker-compose.prod.yml ps
```

The routine release command keeps MySQL running and only recreates application containers. Restart MySQL only when changing MySQL configuration or doing planned database maintenance.

Default local bindings:

- Frontend container: `127.0.0.1:5173 -> frontend:80`
- Backend API/MCP: `127.0.0.1:18080 -> backend:18080`
- Mock TCP range: `127.0.0.1:15400-15409 -> backend:14400-14409`

Set `FAKER_MOCK_ACCESS_HOST` to the host or IP users should call for Mock TCP traffic. This value is shown in the UI for every channel as a deployment access hint; it does not change listener binding.

Web uses HTTP reverse proxy under `/faker/`, so Faker does not take over the whole domain. Include `deploy/nginx/faker-web.conf` inside your existing host Nginx HTTP `server {}` block.

Mock TCP uses Nginx stream forwarding, also known as layer-4 forwarding. Include `deploy/nginx/faker-stream.conf` inside the host Nginx top-level `stream {}` block. The sample exposes 10 public ports, `14400-14409`, and forwards them to local `127.0.0.1:15400-15409`.

```nginx
stream {
    include /path/to/fake-tcp-server/deploy/nginx/faker-stream.conf;
}
```

Check stream support before enabling the TCP config:

```bash
nginx -V 2>&1 | grep -- --with-stream
```

Reload Nginx:

```bash
nginx -t
systemctl reload nginx
```

Open firewall ports as needed:

- `80` or `443` for Web.
- `14400-14409` for Mock TCP channels.

## Channel configuration

Channels are configured in `backend/src/main/resources/application.yml`.

```yaml
fake-tcp:
  mockTls:
    keyStore: ${FAKER_MOCK_TLS_KEY_STORE:}
    keyStoreType: ${FAKER_MOCK_TLS_KEY_STORE_TYPE:PKCS12}
    keyStorePassword: ${FAKER_MOCK_TLS_KEY_STORE_PASSWORD:}
    keyPassword: ${FAKER_MOCK_TLS_KEY_PASSWORD:}
    protocol: ${FAKER_MOCK_TLS_PROTOCOL:TLS}
  channels:
    - id: channel-a
      name: Channel A
      enabled: true
      tcp:
        host: 0.0.0.0
        port: 14400
      framing:
        type: BINARY_2
        byteOrder: BIG_ENDIAN
        lengthIncludes: PAYLOAD
      header:
        enabled: false
        length: 0
        responseMode: NONE
      iso8583:
        packager:
          type: CLASS
          className: org.jpos.iso.packager.ISO87APackager
      noMatch:
        responseCode: "96"
```

Mock listener ports are TLS-only. If `keyStore` is empty, Faker generates an in-memory self-signed certificate on startup. To use a fixed certificate instead, configure `fake-tcp.mockTls` or the `FAKER_MOCK_TLS_*` environment variables. `keyStore` accepts Spring resource locations such as `file:/app/certs/faker-mock.p12` or `classpath:certs/faker-mock.p12`.

Packager options:

```yaml
packager:
  type: XML
  configMode: XML_CONTENT
```

For UI-managed channels, `XML_CONTENT` stores the XML packager body in MySQL. The UI supports pasting XML directly or uploading an XML file and saving its content to the database.

```yaml
packager:
  type: CLASS
  configMode: CLASS_NAME
  className: org.jpos.iso.packager.ISO87APackager
```

```yaml
packager:
  type: CUSTOM
  configMode: JAVA_SOURCE
  className: com.example.iso.ChannelPackager
```

For UI-managed custom packagers, `JAVA_SOURCE` stores the Java source body in MySQL. On backend restart, the service compiles and loads that source. Class-name-only mode is supported but not recommended because the class must be manually implemented before restart.
Custom class and Java source modes must extend `org.jpos.iso.ISOBasePackager`; arbitrary `ISOPackager` implementations are rejected during channel validation and Packager preview.

Filename mode for XML is also supported but not recommended: put the XML under `backend/src/main/resources/packager/` before building, or under the equivalent packaged classpath location in deployment.

## Rule JSON

Example rule:

```json
{
  "name": "Purchase success",
  "enabled": true,
  "capability": "DEBIT",
  "priority": 100,
  "matchMode": "ALL",
  "systemConditions": [
    { "field": "0", "operator": "EQ", "value": "0200" },
    { "field": "3", "operator": "EQ", "value": "000000" }
  ],
  "conditions": [
    { "field": "4", "operator": "EXISTS", "value": "" }
  ],
  "action": {
    "type": "RESPOND",
    "delayMs": 0
  },
  "response": {
    "mti": "0210",
    "fields": {
      "39": { "type": "FIXED", "value": "00" },
      "11": { "type": "COPY_REQUEST_FIELD", "sourceField": "11" },
      "37": { "type": "RANDOM_NUMERIC", "length": 12 },
      "38": { "type": "RANDOM_NUMERIC", "length": 6 }
    }
  }
}
```

Field ids may be entered as plain numbers in UI/API payloads, for example `3` or `39`; semantically they mean ISO8583 data elements `DE3` and `DE39`. The service also accepts `DE3`/`DE39` and normalizes them to the numeric field key internally.

Rules have one `capability`, such as `DEBIT`, `BALANCE_QUERY`, `PRE_AUTH`, `PRE_AUTH_COMPLETION`, `REVERSAL`, `REFUND`, `TID_INIT_9A`, `TID_INIT_9G`, `TID_INIT_9B`, `PARAMETER_DOWNLOAD`, or `CALLHOME`. Capability defaults generate two system preconditions, request `MTI` and `DE3`, plus the default response MTI. System preconditions are always evaluated with AND before user conditions, and are not affected by `matchMode`.

Default POS capability values follow the included POS interface reference: TID-TMK uses `0800/9A0000`, TID-TPK uses `0800/9G0000`, TID-TSK uses `0800/9B0000`, parameter download uses `0800/9C0000`, and Callhome uses `0800/9D0000`.

MAC handling is driven by the ISO8583 message shape. For `ANSI_X9_19`, if the request bitmap/message already contains `DE64` or `DE128`, verification and response signing use that field. If XML only declares the MAC field in the `<field id="bitmap" .../>` value, the serializer uses that bitmap hint when filling the MAC. When no MAC field is present, `PARAMETER_DOWNLOAD` and `CALLHOME` default to `DE64`; other non TID-initialization capabilities default to `DE128`. `SHA256_FIELD128_TRIM64` always uses `DE128`; if `DE128` is missing it is filled with 64 zeroes before packing, the packed ISO bytes are trimmed by removing the last 64 bytes, then the MAC is `SHA-256(hex2byte(TSK) + trimmedPackedBytes)` as 64 uppercase HEX characters. `TID-TMK`, `TID-TPK`, and `TID-TSK` do not require MAC by default.

Supported operators:

- `EQ`, `NE`
- `EXISTS`, `NOT_EXISTS`
- `CONTAINS`, `NOT_CONTAINS`
- `GT`, `GTE`, `LT`, `LTE`
- `REGEX`
- `IN`

Supported actions:

- `RESPOND`
- `DELAY_RESPOND`
- `TIMEOUT`
- `DISCONNECT`

Response field value types:

- `FIXED`
- `COPY_REQUEST_FIELD`
- `CURRENT_DATETIME`
- `RANDOM_NUMERIC`
- `RANDOM_ALPHANUMERIC`

## REST API

```text
GET    /api/channels
GET    /api/channels/{channelId}/rules
POST   /api/channels/{channelId}/rules
PUT    /api/channels/{channelId}/rules/{ruleId}
POST   /api/channels/{channelId}/rules/{ruleId}/enable
POST   /api/channels/{channelId}/rules/{ruleId}/disable
DELETE /api/channels/{channelId}/rules/{ruleId}
POST   /api/channels/{channelId}/rules/test
POST   /api/channels/{channelId}/debug/pos/build-xml
POST   /api/channels/{channelId}/debug/pos/send
POST   /api/channels/{channelId}/debug/pos/tid-init
GET    /api/channels/{channelId}/logs
GET    /api/channels/{channelId}/logs/{logId}
```

Create a rule:

```bash
curl -X POST http://localhost:18080/api/channels/channel-a/rules \
  -H 'Content-Type: application/json' \
  -d @rule.json
```

Dry-run a request:

```bash
curl -X POST http://localhost:18080/api/channels/channel-a/rules/test \
  -H 'Content-Type: application/json' \
  -d '{
    "mti": "0200",
    "fields": {
      "3": "000000",
      "4": "000000010000",
      "11": "123456",
      "41": "TERM0001"
    }
}'
```

## MCP endpoint

The MCP endpoint is available at:

```text
POST http://localhost:18080/mcp
Authorization: Bearer mcp_xxx
Content-Type: application/json
```

When using the bundled frontend or Vite dev server, `/mcp` is also proxied through the UI origin, so the browser-local access path is:

```text
POST http://localhost:5173/mcp
```

Create an MCP token from the user detail page. The token belongs to that user; MCP rule tools only manage that user's own rules, while channel and key configuration remain global data.

Supported JSON-RPC methods:

```text
initialize
notifications/initialized
tools/list
tools/call
ping
```

Main tools:

```text
channel_list
channel_get
channel_create
channel_update
channel_packager_preview
key_get
key_update
rule_capabilities
rule_list
rule_create
rule_update
rule_set_enabled
rule_delete
public_rule_list
public_rule_copy
rule_validate_xml
rule_validation_history
debug_pos_build_xml
debug_pos_send
debug_pos_tid_init
```

POS Debug tools use the channel's global third-party test environment and Packager configuration. `debug_pos_tid_init` only requires `tid` and `sn`; it executes TID-TMK, TID-TPK, and TID-TSK in sequence and does not require PAN/PIN. Ordinary POS debug requests can use `macAlgorithm` and `pinAlgorithm`; `ISO0_3DES_ECB` writes an ISO-0 PIN block to `DE52` using the channel TPK.

When an MCP client creates a channel, call `channel_list` first and recommend the lowest unused port in `14400-14700`. The final `channel_create` call must still provide real per-channel values for `channelCode`, `port`, and the Packager configuration. Do not reuse placeholder channel codes, and do not copy another channel's Packager. After creating or updating a channel Packager, call `channel_packager_preview` to verify the field definitions.

## Notes

- This first version is stateless. Reversal, authorization completion, duplicate detection, balances, and retry counters are intentionally not modeled yet.
- One channel should normally use one TCP port. Shared-port channel identification can be added later if a channel requires it.
- The default no-match response returns response code `96`.
- Channel TCP ports are restricted to `14400-14700`. Invalid channel ports fail application startup during configuration validation.

## MySQL persistence

Docker Compose uses a named volume:

```yaml
volumes:
  mysql-data:
```

That volume is mounted to `/var/lib/mysql`, so rules and request logs survive container recreation and image rebuilds. Flyway manages schema creation and migration on backend startup.

For production-like usage, keep the named volume, avoid `docker compose down -v`, and add a regular backup job such as `mysqldump` or physical MySQL backup. The current Compose setup provides local persistence, not high availability.
