package com.example.faketcp.service;

import com.example.faketcp.dto.ChannelConfigDto;
import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.dto.PosDebugBuildXmlRequestDto;
import com.example.faketcp.dto.PosDebugSendRequestDto;
import com.example.faketcp.dto.PosDebugTidInitRequestDto;
import com.example.faketcp.dto.RuleCapability;
import com.example.faketcp.dto.RuleDto;
import com.example.faketcp.dto.TestHistoryRunRequestDto;
import com.example.faketcp.dto.TestMatchResponse;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.iso.IsoXmlMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class McpService {
    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final ChannelService channelService;
    private final KeySettingsService keySettingsService;
    private final RuleService ruleService;
    private final TestHistoryService testHistoryService;
    private final PosDebugService posDebugService;
    private final IsoXmlMapper isoXmlMapper;
    private final ObjectMapper objectMapper;

    public McpService(
            ChannelService channelService,
            KeySettingsService keySettingsService,
            RuleService ruleService,
            TestHistoryService testHistoryService,
            PosDebugService posDebugService,
            IsoXmlMapper isoXmlMapper,
            ObjectMapper objectMapper) {
        this.channelService = channelService;
        this.keySettingsService = keySettingsService;
        this.ruleService = ruleService;
        this.testHistoryService = testHistoryService;
        this.posDebugService = posDebugService;
        this.isoXmlMapper = isoXmlMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> handle(Map<String, Object> request, UserDto user) {
        Object id = request.get("id");
        String method = stringValue(request.get("method"));
        try {
            if ("initialize".equals(method)) {
                return response(id, initializeResult());
            }
            if ("notifications/initialized".equals(method)) {
                return null;
            }
            if ("tools/list".equals(method)) {
                return response(id, mapOf("tools", tools()));
            }
            if ("tools/call".equals(method)) {
                Map<String, Object> params = objectMap(request.get("params"));
                String name = stringValue(params.get("name"));
                Map<String, Object> arguments = objectMap(params.get("arguments"));
                return response(id, toolResult(callTool(name, arguments, user)));
            }
            if ("ping".equals(method)) {
                return response(id, Collections.emptyMap());
            }
            return error(id, -32601, "Unsupported MCP method: " + method);
        } catch (Exception e) {
            return error(id, -32602, e.getMessage());
        }
    }

    public Map<String, Object> error(Object id, int code, String message) {
        return mapOf(
                "jsonrpc", "2.0",
                "id", id,
                "error", mapOf("code", code, "message", message == null ? "MCP error" : message));
    }

    private Map<String, Object> initializeResult() {
        return mapOf(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", mapOf("tools", mapOf("listChanged", false)),
                "serverInfo", mapOf("name", "fake-tcp-server", "version", "0.1.0"));
    }

    private Object callTool(String name, Map<String, Object> arguments, UserDto user) {
        if ("channel_list".equals(name)) {
            return channelService.list();
        }
        if ("channel_get".equals(name)) {
            return channelService.list().stream()
                    .filter(channel -> channel.getId().equals(requiredString(arguments, "channelId")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown channel: " + requiredString(arguments, "channelId")));
        }
        if ("channel_create".equals(name)) {
            return channelService.create(convert(arguments.get("config"), ChannelConfigDto.class));
        }
        if ("channel_update".equals(name)) {
            return channelService.update(requiredString(arguments, "channelId"), convert(arguments.get("config"), ChannelConfigDto.class));
        }
        if ("channel_packager_preview".equals(name)) {
            return channelService.previewPackager(requiredString(arguments, "channelId"));
        }
        if ("key_get".equals(name)) {
            return keySettingsService.get(requiredString(arguments, "channelId"));
        }
        if ("key_update".equals(name)) {
            String channelId = requiredString(arguments, "channelId");
            KeySettingsDto settings = convert(arguments.get("settings"), KeySettingsDto.class);
            return keySettingsService.save(channelId, settings);
        }
        if ("rule_capabilities".equals(name)) {
            return Arrays.stream(RuleCapability.values())
                    .map(capability -> mapOf(
                            "name", capability.name(),
                            "label", capability.getLabel(),
                            "requestMti", capability.getRequestMti(),
                            "processCode", capability.getProcessCode(),
                            "responseMti", capability.getResponseMti()))
                    .collect(Collectors.toList());
        }
        if ("rule_list".equals(name)) {
            String channelId = requiredString(arguments, "channelId");
            RuleCapability capability = enumValue(arguments.get("capability"), RuleCapability.class);
            return ruleService.list(channelId, capability, user);
        }
        if ("rule_create".equals(name)) {
            String channelId = requiredString(arguments, "channelId");
            RuleDto rule = convert(arguments.get("rule"), RuleDto.class);
            return ruleService.create(channelId, rule, user);
        }
        if ("rule_update".equals(name)) {
            String channelId = requiredString(arguments, "channelId");
            long ruleId = requiredLong(arguments, "ruleId");
            RuleDto rule = convert(arguments.get("rule"), RuleDto.class);
            return ruleService.update(channelId, ruleId, rule, user);
        }
        if ("rule_set_enabled".equals(name)) {
            String channelId = requiredString(arguments, "channelId");
            long ruleId = requiredLong(arguments, "ruleId");
            ruleService.setEnabled(channelId, ruleId, requiredBoolean(arguments, "enabled"), user);
            return mapOf("channelId", channelId, "ruleId", ruleId, "enabled", requiredBoolean(arguments, "enabled"));
        }
        if ("rule_delete".equals(name)) {
            String channelId = requiredString(arguments, "channelId");
            long ruleId = requiredLong(arguments, "ruleId");
            ruleService.delete(channelId, ruleId, user);
            return mapOf("channelId", channelId, "ruleId", ruleId, "deleted", true);
        }
        if ("public_rule_list".equals(name)) {
            return ruleService.publicRules();
        }
        if ("public_rule_copy".equals(name)) {
            return ruleService.copyPublicRule(requiredString(arguments, "channelId"), requiredLong(arguments, "ruleId"), user);
        }
        if ("rule_validate_xml".equals(name)) {
            return validateXml(arguments, user);
        }
        if ("rule_validation_history".equals(name)) {
            return testHistoryService.list(user.getId(), requiredString(arguments, "channelId"));
        }
        if ("debug_pos_build_xml".equals(name)) {
            return posDebugService.buildXml(
                    requiredString(arguments, "channelId"),
                    convert(arguments.get("request"), PosDebugBuildXmlRequestDto.class));
        }
        if ("debug_pos_send".equals(name)) {
            return posDebugService.send(
                    requiredString(arguments, "channelId"),
                    convert(arguments.get("request"), PosDebugSendRequestDto.class));
        }
        if ("debug_pos_tid_init".equals(name)) {
            return posDebugService.tidInit(
                    requiredString(arguments, "channelId"),
                    convert(arguments.get("request"), PosDebugTidInitRequestDto.class));
        }
        throw new IllegalArgumentException("Unknown MCP tool: " + name);
    }

    private Object validateXml(Map<String, Object> arguments, UserDto user) {
        String channelId = requiredString(arguments, "channelId");
        String requestXml = requiredString(arguments, "requestXml");
        boolean saveHistory = optionalBoolean(arguments, "saveHistory", true);
        if (saveHistory) {
            TestHistoryRunRequestDto request = new TestHistoryRunRequestDto();
            request.setRequestXml(requestXml);
            return testHistoryService.run(user.getId(), channelId, request);
        }
        IsoMessageDto request = isoXmlMapper.parse(requestXml);
        TestMatchResponse response = ruleService.test(channelId, request, user);
        return mapOf(
                "matched", response.isMatched(),
                "rule", response.getRule(),
                "action", response.getAction(),
                "response", response.getResponse(),
                "responseXml", isoXmlMapper.render(response.getResponse()));
    }

    private Map<String, Object> toolResult(Object structuredContent) throws JsonProcessingException {
        String text = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(structuredContent);
        return mapOf(
                "content", Collections.singletonList(mapOf("type", "text", "text", text)),
                "structuredContent", structuredContent);
    }

    private List<Map<String, Object>> tools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(tool("channel_list", "列出所有全局渠道配置。", objectSchema()));
        tools.add(tool("channel_get", "读取一个渠道的完整配置和生效信息。", objectSchema(Collections.singletonList(
                prop("channelId", "string", "渠道 ID")))));
        tools.add(tool("channel_create", "创建全局渠道配置。MCP client 应先调用 channel_list 查看已配置端口，再选择 14400-14700 内未被占用的端口；必须逐渠道明确设置 config.channelCode、config.port 和 Packager 配置，不要复用示例或其它渠道的 Packager。新增 TCP listener 需要重启后端生效。", objectSchema(Collections.singletonList(
                prop("config", "object", "ChannelConfigDto 渠道配置对象。channelCode 必须是真实渠道编码；port 必须是 14400-14700 内唯一监听端口，可根据 channel_list 结果推荐最低空闲端口；Packager 必须选择 XML 内容/文件、jPOS 类名或自定义 ISOBasePackager，且必须是该渠道自己的正确方言。保存后建议调用 channel_packager_preview 验证字段定义。")))));
        tools.add(tool("channel_update", "更新全局渠道配置。修改 channelCode、port 或 Packager 前必须确认当前渠道真实值；Packager 不能复用其它渠道配置。接口会返回是否需要重启。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("config", "object", "ChannelConfigDto 渠道配置对象。保存 Packager 变更后建议调用 channel_packager_preview 验证字段定义。")))));
        tools.add(tool("channel_packager_preview", "预览渠道 Packager 字段定义。", objectSchema(Collections.singletonList(
                prop("channelId", "string", "渠道 ID")))));
        tools.add(tool("key_get", "读取指定渠道的全局 Key 设置。包含明文 TPK/TSK、MAC 策略和测试交易参数。", objectSchema(Collections.singletonList(
                prop("channelId", "string", "渠道 ID")))));
        tools.add(tool("key_update", "保存指定渠道的全局 Key 设置；热生效，不需要重启。settings 使用 KeySettingsDto 字段，macAlgorithm 支持 ANSI_X9_19 和 SHA256_FIELD128_TRIM64。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("settings", "object", "KeySettingsDto，支持 tpkPlain、tskPlain、macAlgorithm、testTid、testPan、testPin、testDe14、testDe42、testDe18、testDe43、testDe49 等字段")))));
        tools.add(tool("rule_capabilities", "列出规则支持的业务能力、默认请求 MTI、DE3 和响应 MTI。", objectSchema()));
        tools.add(tool("rule_list", "列出当前 MCP 用户在指定渠道下的规则。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                optionalProp("capability", "string", "可选，RuleCapability 枚举名")))));
        tools.add(tool("rule_create", "为当前 MCP 用户创建规则；RuleDto.systemConditions 可传 field=0/3 的取值，response.mti 可自定义。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("rule", "object", "RuleDto 规则对象")))));
        tools.add(tool("rule_update", "更新当前 MCP 用户自己的规则；允许修改能力前提 field=0/3 的取值和 response.mti。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("ruleId", "number", "规则 ID"),
                prop("rule", "object", "RuleDto 规则对象")))));
        tools.add(tool("rule_set_enabled", "启用或停用当前 MCP 用户自己的规则。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("ruleId", "number", "规则 ID"),
                prop("enabled", "boolean", "是否启用")))));
        tools.add(tool("rule_delete", "删除当前 MCP 用户自己的规则。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("ruleId", "number", "规则 ID")))));
        tools.add(tool("public_rule_list", "列出全局公开规则库。", objectSchema()));
        tools.add(tool("public_rule_copy", "复制公开规则为当前 MCP 用户自己的规则。", objectSchema(Arrays.asList(
                prop("channelId", "string", "目标渠道 ID"),
                prop("ruleId", "number", "公开规则 ID")))));
        tools.add(tool("rule_validate_xml", "使用 XML 请求执行规则验证；默认保存到当前 MCP 用户最近 30 条验证历史。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("requestXml", "string", "ISO8583 XML 请求"),
                optionalProp("saveHistory", "boolean", "可选，默认 true")))));
        tools.add(tool("rule_validation_history", "查看当前 MCP 用户在指定渠道下最近 30 条规则验证历史。", objectSchema(Collections.singletonList(
                prop("channelId", "string", "渠道 ID")))));
        tools.add(tool("debug_pos_build_xml", "按 POS 能力生成 ISO8583 XML 请求；自动填充 MTI、DE3、STAN、时间戳、RRN，并根据手册 mandatory 字段补全骨架，取不到值的必填域输出空字符串。TID 初始化使用环境变量 TID/SN；request.environmentFields 用于 DE2/DE14/DE41/DE52/SN 等环境变量，request.dynamicFields 用于 DE18/DE42/DE43/DE49 等动态参数，request.fields 为用户手工覆盖层。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("request", "object", "PosDebugBuildXmlRequestDto，包含 capability、tid、sn、environmentFields、dynamicFields、fields")))));
        tools.add(tool("debug_pos_send", "向渠道配置的第三方测试环境发送 POS ISO8583 请求，并返回请求 HEX、响应 XML、DE39、响应 MAC 验证结果和 Debug 报告。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("request", "object", "PosDebugSendRequestDto，包含 capability、requestXml、macAlgorithm、pinAlgorithm、pan、pin、saveTidConfig")))));
        tools.add(tool("debug_pos_tid_init", "一次性执行 TID-TMK、TID-TPK、TID-TSK 三步初始化；只需要 tid 和 sn，不需要 PAN/PIN。", objectSchema(Arrays.asList(
                prop("channelId", "string", "渠道 ID"),
                prop("request", "object", "PosDebugTidInitRequestDto，包含 tid、sn、saveKey")))));
        return tools;
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> inputSchema) {
        return mapOf("name", name, "description", description, "inputSchema", inputSchema);
    }

    private Map<String, Object> objectSchema() {
        return objectSchema(Collections.emptyList());
    }

    private Map<String, Object> objectSchema(List<Map<String, Object>> properties) {
        Map<String, Object> schemaProperties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Map<String, Object> property : properties) {
            String name = stringValue(property.get("name"));
            schemaProperties.put(name, mapOf(
                    "type", property.get("type"),
                    "description", property.get("description")));
            if (!Boolean.FALSE.equals(property.get("required"))) {
                required.add(name);
            }
        }
        Map<String, Object> schema = mapOf(
                "type", "object",
                "properties", schemaProperties,
                "additionalProperties", false);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private Map<String, Object> prop(String name, String type, String description) {
        return mapOf("name", name, "type", type, "description", description, "required", true);
    }

    private Map<String, Object> optionalProp(String name, String type, String description) {
        return mapOf("name", name, "type", type, "description", description, "required", false);
    }

    private Map<String, Object> response(Object id, Object result) {
        return mapOf("jsonrpc", "2.0", "id", id, "result", result);
    }

    private <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            throw new IllegalArgumentException("Missing object argument");
        }
        return objectMapper.convertValue(value, type);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    private String requiredString(Map<String, Object> arguments, String name) {
        String value = stringValue(arguments.get(name));
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required argument: " + name);
        }
        return value.trim();
    }

    private long requiredLong(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(requiredString(arguments, name));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Argument must be a number: " + name);
        }
    }

    private boolean requiredBoolean(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        throw new IllegalArgumentException("Argument must be a boolean: " + name);
    }

    private boolean optionalBoolean(Map<String, Object> arguments, String name, boolean defaultValue) {
        if (!arguments.containsKey(name)) {
            return defaultValue;
        }
        Object value = arguments.get(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private <E extends Enum<E>> E enumValue(Object value, Class<E> type) {
        String text = stringValue(value);
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return Enum.valueOf(type, text.trim());
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
