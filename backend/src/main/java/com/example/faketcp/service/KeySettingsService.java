package com.example.faketcp.service;

import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.repository.KeySettingsRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class KeySettingsService {
    private static final String ANSI_X9_19 = "ANSI_X9_19";
    private static final String SHA256_FIELD128_TRIM64 = "SHA256_FIELD128_TRIM64";
    private static final Set<String> SUPPORTED_MAC_ALGORITHMS =
            new HashSet<>(Arrays.asList(ANSI_X9_19, SHA256_FIELD128_TRIM64));

    private final ChannelService channelService;
    private final KeySettingsRepository keySettingsRepository;

    public KeySettingsService(ChannelService channelService, KeySettingsRepository keySettingsRepository) {
        this.channelService = channelService;
        this.keySettingsRepository = keySettingsRepository;
    }

    public KeySettingsDto get(String channelId) {
        channelService.getRequired(channelId);
        return normalize(keySettingsRepository.findByChannel(channelId));
    }

    public KeySettingsDto save(String channelId, KeySettingsDto settings) {
        channelService.getRequired(channelId);
        KeySettingsDto normalized = normalize(settings);
        validateKey("TPK", normalized.getTpkPlain(), false);
        validateKey("TSK", normalized.getTskPlain(), false);
        if (!"64".equals(normalized.getMacField()) && !"128".equals(normalized.getMacField())) {
            throw new IllegalStateException("MAC 字段只支持 64 或 128");
        }
        if (!SUPPORTED_MAC_ALGORITHMS.contains(normalized.getMacAlgorithm())) {
            throw new IllegalStateException("MAC 算法只支持 ANSI_X9_19 或 SHA256_FIELD128_TRIM64");
        }
        validateMaxLength("测试 TID", normalized.getTestTid(), 8);
        validatePattern("测试 PAN", normalized.getTestPan(), "[0-9]{1,19}", "必须是 1 到 19 位数字");
        validateMaxLength("测试 PIN 信息", normalized.getTestPin(), 255);
        validatePattern("DE14 卡号有效期", normalized.getTestDe14(), "[0-9]{4}", "必须是 YYMM 格式的 4 位数字");
        validateMaxLength("DE42", normalized.getTestDe42(), 15);
        validatePattern("DE18", normalized.getTestDe18(), "[0-9]{4}", "必须是 4 位数字");
        validateMaxLength("DE43", normalized.getTestDe43(), 40);
        validatePattern("DE49", normalized.getTestDe49(), "[0-9]{3}", "必须是 3 位数字");
        return normalize(keySettingsRepository.save(channelId, normalized));
    }

    private KeySettingsDto normalize(KeySettingsDto settings) {
        KeySettingsDto normalized = settings == null ? new KeySettingsDto() : settings;
        normalized.setTpkPlain(clean(normalized.getTpkPlain()));
        normalized.setTskPlain(clean(normalized.getTskPlain()));
        normalized.setMacField(defaultIfBlank(normalized.getMacField(), "128"));
        normalized.setMacAlgorithm(normalizeMacAlgorithm(normalized.getMacAlgorithm()));
        normalized.setTestTid(trimToNull(normalized.getTestTid()));
        normalized.setTestPan(trimToNull(normalized.getTestPan()));
        normalized.setTestPin(trimToNull(normalized.getTestPin()));
        normalized.setTestDe14(trimToNull(normalized.getTestDe14()));
        normalized.setTestDe42(trimToNull(normalized.getTestDe42()));
        normalized.setTestDe18(trimToNull(normalized.getTestDe18()));
        normalized.setTestDe43(trimToNull(normalized.getTestDe43()));
        normalized.setTestDe49(trimToNull(normalized.getTestDe49()));
        return normalized;
    }

    private void validateKey(String name, String value, boolean required) {
        if (value == null || value.isEmpty()) {
            if (required) {
                throw new IllegalStateException(name + " 不能为空");
            }
            return;
        }
        if (!value.matches("[0-9A-Fa-f]+") || (value.length() != 16 && value.length() != 32 && value.length() != 48)) {
            throw new IllegalStateException(name + " 必须是 16、32 或 48 位 HEX 明文密钥");
        }
    }

    private String clean(String value) {
        return value == null ? null : value.replaceAll("\\s+", "").toUpperCase();
    }

    private void validateMaxLength(String name, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalStateException(name + " 最多 " + maxLength + " 位");
        }
    }

    private void validatePattern(String name, String value, String pattern, String message) {
        if (value != null && !value.matches(pattern)) {
            throw new IllegalStateException(name + " " + message);
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private String normalizeMacAlgorithm(String value) {
        return defaultIfBlank(value, ANSI_X9_19).toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
