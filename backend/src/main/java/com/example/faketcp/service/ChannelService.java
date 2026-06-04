package com.example.faketcp.service;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.config.FakeTcpProperties;
import com.example.faketcp.config.FramingProperties;
import com.example.faketcp.config.HeaderProperties;
import com.example.faketcp.config.PackagerProperties;
import com.example.faketcp.dto.ChannelConfigDto;
import com.example.faketcp.dto.ChannelDto;
import com.example.faketcp.dto.PackagerFieldDto;
import com.example.faketcp.dto.PackagerPreviewDto;
import com.example.faketcp.iso.IsoFieldReferences;
import com.example.faketcp.iso.IsoFieldValueType;
import com.example.faketcp.iso.PackagerFactory;
import com.example.faketcp.repository.ChannelRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOFieldPackager;
import org.springframework.stereotype.Service;

@Service
public class ChannelService {
    private static final List<String> RESTART_REQUIRED_FIELDS = Arrays.asList(
            "enabled",
            "host",
            "port",
            "framingType",
            "byteOrder",
            "lengthIncludes",
            "headerEnabled",
            "headerLength",
            "headerResponseMode",
            "headerFixedValueHex",
            "packagerType",
            "packagerConfigMode",
            "packagerLocation",
            "packagerFileName",
            "packagerContent",
            "packagerClassName",
            "mockTlsEnabled",
            "noMatchResponseCode");
    private static final List<String> HOT_EFFECTIVE_FIELDS = Arrays.asList(
            "channelCode",
            "name",
            "thirdPartyTestIp",
            "thirdPartyTestPort",
            "thirdPartyTlsEnabled",
            "ctmk1",
            "ctmk2",
            "mockCtmk1",
            "mockCtmk2");

    private final Map<String, ChannelProperties> staticChannels;
    private final String mockAccessHost;
    private final ChannelRepository channelRepository;
    private final PackagerFactory packagerFactory;

    public ChannelService(FakeTcpProperties properties, ChannelRepository channelRepository, PackagerFactory packagerFactory) {
        this.staticChannels = properties.getChannels().stream()
                .collect(Collectors.toMap(ChannelProperties::getId, Function.identity()));
        this.mockAccessHost = blankToDefault(properties.getMockAccess() == null ? null : properties.getMockAccess().getHost(), "127.0.0.1");
        this.channelRepository = channelRepository;
        this.packagerFactory = packagerFactory;
    }

    public List<ChannelDto> list() {
        List<ChannelDto> channels = new ArrayList<>();
        List<ChannelConfigDto> databaseChannels = channelRepository.findAll();
        List<String> databaseChannelIds = databaseChannels.stream()
                .map(ChannelConfigDto::getId)
                .collect(Collectors.toList());
        for (ChannelProperties channel : staticChannels.values()) {
            if (!databaseChannelIds.contains(channel.getId())) {
                channels.add(toDto(channel, "application.yml", false));
            }
        }
        for (ChannelConfigDto channel : databaseChannels) {
            channels.add(toDto(channel, "database", false));
        }
        return channels;
    }

    public ChannelProperties getRequired(String channelId) {
        ChannelProperties channel = allChannels().get(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Unknown channel: " + channelId);
        }
        return channel;
    }

    public List<ChannelProperties> enabledChannels() {
        return allChannels().values().stream().filter(ChannelProperties::isEnabled).collect(Collectors.toList());
    }

    public ChannelDto create(ChannelConfigDto channel) {
        prepareWritableChannel(channel, null);
        validateUniqueCode(channel.getChannelCode(), null);
        validateUniquePort(channel.getPort(), null);
        validateUniquePackager(channel, null);
        return toDto(
                channelRepository.create(channel),
                "database",
                true,
                Collections.singletonList("新增渠道需要创建 TCP listener"));
    }

    public ChannelDto update(String channelId, ChannelConfigDto channel) {
        ChannelConfigDto before = channelRepository.existsById(channelId)
                ? channelRepository.findById(channelId)
                : toConfig(getRequired(channelId), staticChannels.containsKey(channelId) ? "application.yml" : "database");
        prepareWritableChannel(channel, channelId);
        validateUniqueCode(channel.getChannelCode(), channelId);
        validateUniquePort(channel.getPort(), channelId);
        if (!Objects.equals(packagerFingerprint(before), packagerFingerprint(channel))) {
            validateUniquePackager(channel, channelId);
        }
        List<String> restartReasons = restartReasons(before, channel);
        ChannelConfigDto saved = channelRepository.existsById(channelId)
                ? channelRepository.update(channelId, channel)
                : channelRepository.create(channel);
        return toDto(
                saved,
                "database",
                !restartReasons.isEmpty(),
                restartReasons);
    }

    public void delete(String channelId) {
        if (staticChannels.containsKey(channelId)) {
            throw new IllegalArgumentException("Static application.yml channels cannot be deleted from UI");
        }
        channelRepository.delete(channelId);
    }

    public PackagerPreviewDto previewPackager(String channelId) {
        ChannelProperties channel = getRequired(channelId);
        return previewPackager(channel);
    }

    public PackagerPreviewDto previewPackager(ChannelConfigDto channel) {
        try {
            return previewPackager(toProperties(channel));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("渠道配置无效: " + e.getMessage(), e);
        }
    }

    public Map<String, IsoFieldValueType> fieldValueTypes(String channelId) {
        ChannelProperties channel = getRequired(channelId);
        ISOBasePackager packager = packagerFactory.create(channel.getIso8583().getPackager());
        return fieldValueTypes(packager);
    }

    public Map<String, IsoFieldValueType> fieldValueTypes(ISOBasePackager packager) {
        if (packager == null) {
            return Collections.emptyMap();
        }
        Map<String, IsoFieldValueType> fieldTypes = new LinkedHashMap<>();
        for (int id = 0; id <= 192; id++) {
            ISOFieldPackager fieldPackager = fieldPackager(packager, id);
            if (fieldPackager != null) {
                fieldTypes.put(IsoFieldReferences.canonical(String.valueOf(id)), IsoFieldValueType.from(fieldPackager));
            }
        }
        return fieldTypes;
    }

    private PackagerPreviewDto previewPackager(ChannelProperties channel) {
        ISOBasePackager packager = packagerFactory.create(channel.getIso8583().getPackager());
        PackagerPreviewDto preview = new PackagerPreviewDto();
        preview.setChannelId(channel.getId());
        preview.setChannelName(channelCode(channel, staticChannels.containsKey(channel.getId()) ? "application.yml" : "database"));
        preview.setPackagerClass(packager.getClass().getName());
        preview.setDescription(packager.getDescription());

        List<PackagerFieldDto> fields = new ArrayList<>();
        for (int id = 0; id <= 192; id++) {
            ISOFieldPackager fieldPackager = fieldPackager(packager, id);
            if (fieldPackager == null) {
                continue;
            }
            PackagerFieldDto field = new PackagerFieldDto();
            field.setId(id);
            field.setType(fieldPackager.getClass().getSimpleName());
            field.setValueType(IsoFieldValueType.from(fieldPackager).name());
            field.setLength(fieldPackager.getLength());
            field.setMaxPackedLength(maxPackedLength(fieldPackager));
            field.setDescription(fieldPackager.getDescription());
            fields.add(field);
        }
        preview.setFields(fields);
        return preview;
    }

    private Map<String, ChannelProperties> allChannels() {
        Map<String, ChannelProperties> merged = new LinkedHashMap<>();
        merged.putAll(staticChannels);
        for (ChannelConfigDto channel : channelRepository.findAll()) {
            merged.put(channel.getId(), toProperties(channel));
        }
        return merged;
    }

    private ChannelDto toDto(ChannelProperties channel, String source, boolean restartRequired) {
        String channelCode = channelCode(channel, source);
        ChannelDto dto = new ChannelDto();
        dto.setId(channel.getId());
        dto.setChannelCode(channelCode);
        dto.setName(channelCode);
        dto.setEnabled(channel.isEnabled());
        dto.setHost(channel.getTcp().getHost());
        dto.setPort(channel.getTcp().getPort());
        dto.setFraming(channel.getFraming().getType().name());
        if (channel.getIso8583().getPackager().getLocation() != null) {
            dto.setPackager(channel.getIso8583().getPackager().getLocation());
        } else {
            dto.setPackager(channel.getIso8583().getPackager().getClassName());
        }
        dto.setMockAccessHost(mockAccessHost);
        dto.setSource(source);
        dto.setRestartRequired(restartRequired);
        dto.setRestartRequiredFields(RESTART_REQUIRED_FIELDS);
        dto.setHotEffectiveFields(HOT_EFFECTIVE_FIELDS);
        dto.setConfig(toConfig(channel, source));
        return dto;
    }

    private ChannelDto toDto(ChannelConfigDto config, String source, boolean restartRequired) {
        return toDto(config, source, restartRequired, Collections.emptyList());
    }

    private ChannelDto toDto(ChannelConfigDto config, String source, boolean restartRequired, List<String> restartReasons) {
        ChannelDto dto = toDto(toProperties(config), source, restartRequired);
        dto.setThirdPartyTestIp(config.getThirdPartyTestIp());
        dto.setThirdPartyTestPort(config.getThirdPartyTestPort());
        dto.setThirdPartyTlsEnabled(config.isThirdPartyTlsEnabled());
        dto.setMockTlsEnabled(config.getMockTlsEnabled());
        dto.setRestartReasons(restartReasons);
        dto.setConfig(config);
        return dto;
    }

    private List<String> restartReasons(ChannelConfigDto before, ChannelConfigDto after) {
        List<String> reasons = new ArrayList<>();
        addRestartReason(reasons, before.isEnabled(), after.isEnabled(), "启用状态变更需要启动或停止 TCP listener");
        if (!Objects.equals(before.getHost(), after.getHost()) || !Objects.equals(before.getPort(), after.getPort())) {
            reasons.add("监听地址或端口变更需要重建 TCP listener");
        }
        if (!Objects.equals(before.getMockTlsEnabled(), after.getMockTlsEnabled())) {
            reasons.add("Mock TLS 变更需要重建 TCP listener");
        }
        if (!Objects.equals(before.getFramingType(), after.getFramingType())
                || !Objects.equals(before.getByteOrder(), after.getByteOrder())
                || !Objects.equals(before.getLengthIncludes(), after.getLengthIncludes())) {
            reasons.add("Framing 变更需要重建 TCP runtime");
        }
        if (before.isHeaderEnabled() != after.isHeaderEnabled()
                || before.getHeaderLength() != after.getHeaderLength()
                || !Objects.equals(before.getHeaderResponseMode(), after.getHeaderResponseMode())
                || !Objects.equals(before.getHeaderFixedValueHex(), after.getHeaderFixedValueHex())) {
            reasons.add("Header/TPDU 变更需要重建 TCP runtime");
        }
        if (!Objects.equals(before.getPackagerType(), after.getPackagerType())
                || !Objects.equals(before.getPackagerConfigMode(), after.getPackagerConfigMode())
                || !Objects.equals(before.getPackagerLocation(), after.getPackagerLocation())
                || !Objects.equals(before.getPackagerFileName(), after.getPackagerFileName())
                || !Objects.equals(before.getPackagerContent(), after.getPackagerContent())
                || !Objects.equals(before.getPackagerClassName(), after.getPackagerClassName())) {
            reasons.add("Packager/方言变更需要重建 Packager 和字段类型缓存");
        }
        if (!Objects.equals(before.getNoMatchResponseCode(), after.getNoMatchResponseCode())) {
            reasons.add("默认响应码变更需要重建 TCP runtime");
        }
        return reasons;
    }

    private void addRestartReason(List<String> reasons, boolean before, boolean after, String reason) {
        if (before != after) {
            reasons.add(reason);
        }
    }

    private ChannelProperties toProperties(ChannelConfigDto dto) {
        String channelCode = channelCode(dto);
        ChannelProperties channel = new ChannelProperties();
        channel.setId(isBlank(dto.getId()) ? channelCode : dto.getId());
        channel.setName(channelCode);
        channel.setEnabled(dto.isEnabled());
        channel.getTcp().setHost(dto.getHost());
        channel.getTcp().setPort(dto.getPort() == null ? 0 : dto.getPort());
        channel.getTcp().setTlsEnabled(effectiveMockTlsEnabled(dto));
        channel.getFraming().setType(FramingProperties.FramingType.valueOf(dto.getFramingType()));
        channel.getFraming().setByteOrder(FramingProperties.ByteOrder.valueOf(dto.getByteOrder()));
        channel.getFraming().setLengthIncludes(FramingProperties.LengthIncludes.valueOf(dto.getLengthIncludes()));
        channel.getHeader().setEnabled(dto.isHeaderEnabled());
        channel.getHeader().setLength(dto.getHeaderLength());
        channel.getHeader().setResponseMode(HeaderProperties.HeaderResponseMode.valueOf(dto.getHeaderResponseMode()));
        channel.getHeader().setFixedValueHex(dto.getHeaderFixedValueHex());
        channel.getIso8583().getPackager().setType(PackagerProperties.PackagerType.valueOf(dto.getPackagerType()));
        channel.getIso8583().getPackager().setConfigMode(PackagerProperties.PackagerConfigMode.valueOf(dto.getPackagerConfigMode()));
        channel.getIso8583().getPackager().setLocation(dto.getPackagerLocation());
        channel.getIso8583().getPackager().setFileName(dto.getPackagerFileName());
        channel.getIso8583().getPackager().setContent(dto.getPackagerContent());
        channel.getIso8583().getPackager().setClassName(dto.getPackagerClassName());
        channel.getNoMatch().setResponseCode(dto.getNoMatchResponseCode());
        return channel;
    }

    private ChannelConfigDto toConfig(ChannelProperties channel, String source) {
        String channelCode = channelCode(channel, source);
        ChannelConfigDto dto = new ChannelConfigDto();
        dto.setId(channel.getId());
        dto.setChannelCode(channelCode);
        dto.setName(channelCode);
        dto.setEnabled(channel.isEnabled());
        dto.setHost(channel.getTcp().getHost());
        dto.setPort(channel.getTcp().getPort());
        dto.setFramingType(channel.getFraming().getType().name());
        dto.setByteOrder(channel.getFraming().getByteOrder().name());
        dto.setLengthIncludes(channel.getFraming().getLengthIncludes().name());
        dto.setHeaderEnabled(channel.getHeader().isEnabled());
        dto.setHeaderLength(channel.getHeader().getLength());
        dto.setHeaderResponseMode(channel.getHeader().getResponseMode().name());
        dto.setHeaderFixedValueHex(channel.getHeader().getFixedValueHex());
        dto.setPackagerType(channel.getIso8583().getPackager().getType().name());
        dto.setPackagerConfigMode(channel.getIso8583().getPackager().getConfigMode().name());
        dto.setPackagerLocation(channel.getIso8583().getPackager().getLocation());
        dto.setPackagerFileName(channel.getIso8583().getPackager().getFileName());
        dto.setPackagerContent(channel.getIso8583().getPackager().getContent());
        dto.setPackagerClassName(channel.getIso8583().getPackager().getClassName());
        dto.setNoMatchResponseCode(channel.getNoMatch().getResponseCode());
        return dto;
    }

    private void prepareWritableChannel(ChannelConfigDto channel, String channelId) {
        String channelCode = channelCode(channel);
        if (isBlank(channelCode)) {
            throw new IllegalStateException("渠道 code 不能为空");
        }
        if (channel.getPort() == null) {
            throw new IllegalStateException("监听端口不能为空；创建渠道时必须为当前渠道明确设置唯一端口");
        }
        if (channel.getPort() < 14400 || channel.getPort() > 14700) {
            throw new IllegalStateException("监听端口必须在 14400 到 14700 之间");
        }
        channel.setId(channelId);
        channel.setChannelCode(channelCode);
        channel.setName(channelCode);
        normalizeThirdPartyTestEnv(channel);
        normalizeMockTestEnv(channel);
        try {
            FramingProperties.FramingType.valueOf(channel.getFramingType());
            FramingProperties.ByteOrder.valueOf(channel.getByteOrder());
            FramingProperties.LengthIncludes.valueOf(channel.getLengthIncludes());
            HeaderProperties.HeaderResponseMode.valueOf(channel.getHeaderResponseMode());
            PackagerProperties.PackagerType.valueOf(channel.getPackagerType());
            PackagerProperties.PackagerConfigMode.valueOf(channel.getPackagerConfigMode());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("渠道配置枚举值不合法: " + e.getMessage(), e);
        }
        validatePackagerIsExplicit(channel);
        packagerFactory.create(toProperties(channel).getIso8583().getPackager());
    }

    private void validatePackagerIsExplicit(ChannelConfigDto channel) {
        PackagerProperties.PackagerType type = PackagerProperties.PackagerType.valueOf(channel.getPackagerType());
        PackagerProperties.PackagerConfigMode mode = PackagerProperties.PackagerConfigMode.valueOf(channel.getPackagerConfigMode());
        if (type == PackagerProperties.PackagerType.XML && mode == PackagerProperties.PackagerConfigMode.XML_CONTENT
                && isBlank(channel.getPackagerContent())) {
            throw new IllegalStateException("Packager XML 内容不能为空；每个渠道必须明确提供自己的方言");
        }
        if (type == PackagerProperties.PackagerType.XML && mode == PackagerProperties.PackagerConfigMode.XML_FILE
                && isBlank(channel.getPackagerFileName()) && isBlank(channel.getPackagerLocation())) {
            throw new IllegalStateException("Packager XML 文件名不能为空；每个渠道必须明确提供自己的方言");
        }
        if ((type == PackagerProperties.PackagerType.CLASS || mode == PackagerProperties.PackagerConfigMode.CLASS_NAME)
                && isBlank(channel.getPackagerClassName())) {
            throw new IllegalStateException("Packager 类名不能为空；每个渠道必须明确提供自己的方言");
        }
        if (type == PackagerProperties.PackagerType.CUSTOM && mode == PackagerProperties.PackagerConfigMode.JAVA_SOURCE
                && isBlank(channel.getPackagerContent())) {
            throw new IllegalStateException("Packager Java 源码不能为空；每个渠道必须明确提供自己的方言");
        }
    }

    private void normalizeThirdPartyTestEnv(ChannelConfigDto channel) {
        channel.setThirdPartyTestIp(blankToNull(channel.getThirdPartyTestIp()));
        channel.setCtmk1(cleanHex(channel.getCtmk1()));
        channel.setCtmk2(cleanHex(channel.getCtmk2()));
        Integer testPort = channel.getThirdPartyTestPort();
        if (isBlank(channel.getThirdPartyTestIp())) {
            throw new IllegalStateException("第三方测试环境 IP 不能为空");
        }
        if (testPort == null) {
            throw new IllegalStateException("第三方测试环境端口不能为空");
        }
        if (testPort < 1 || testPort > 65535) {
            throw new IllegalStateException("第三方测试环境端口必须在 1 到 65535 之间");
        }
        if (isBlank(channel.getCtmk1())) {
            throw new IllegalStateException("第三方测试环境 CTMK1 不能为空");
        }
        if (isBlank(channel.getCtmk2())) {
            throw new IllegalStateException("第三方测试环境 CTMK2 不能为空");
        }
        validateCtmk("CTMK1", channel.getCtmk1());
        validateCtmk("CTMK2", channel.getCtmk2());
    }

    private void normalizeMockTestEnv(ChannelConfigDto channel) {
        channel.setMockCtmk1(cleanHex(channel.getMockCtmk1()));
        channel.setMockCtmk2(cleanHex(channel.getMockCtmk2()));
        if (isBlank(channel.getMockCtmk1())) {
            channel.setMockCtmk1(channel.getCtmk1());
        }
        if (isBlank(channel.getMockCtmk2())) {
            channel.setMockCtmk2(channel.getCtmk2());
        }
        validateCtmk("Mock CTMK1", channel.getMockCtmk1());
        validateCtmk("Mock CTMK2", channel.getMockCtmk2());
    }

    private boolean effectiveMockTlsEnabled(ChannelConfigDto channel) {
        return channel.getMockTlsEnabled() == null ? channel.isThirdPartyTlsEnabled() : channel.getMockTlsEnabled();
    }

    private void validateCtmk(String name, String value) {
        if (isBlank(value)) {
            return;
        }
        if (!value.matches("[0-9A-F]+") || value.length() < 16 || value.length() > 64 || value.length() % 2 != 0) {
            throw new IllegalStateException(name + " 必须是 16 到 64 位偶数长度 HEX");
        }
    }

    private String cleanHex(String value) {
        return isBlank(value) ? null : value.replaceAll("\\s+", "").toUpperCase();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private void validateUniqueCode(String channelCode, String channelId) {
        String normalizedCode = channelCode(channelCode);
        boolean staticCodeExists = staticChannels.values().stream()
                .anyMatch(channel -> !channel.getId().equals(channelId) && normalizedCode.equals(channelCode(channel, "application.yml")));
        if (staticCodeExists || channelRepository.existsByCode(normalizedCode, channelId)) {
            throw new IllegalStateException("渠道 code 已存在");
        }
    }

    private void validateUniquePort(Integer port, String channelId) {
        boolean staticPortExists = staticChannels.values().stream()
                .anyMatch(channel -> !channel.getId().equals(channelId) && channel.getTcp().getPort() == port);
        if (staticPortExists || channelRepository.existsByPort(port, channelId)) {
            throw new IllegalStateException("监听端口已被其他渠道占用");
        }
    }

    private void validateUniquePackager(ChannelConfigDto channel, String channelId) {
        String fingerprint = packagerFingerprint(channel);
        boolean staticPackagerExists = staticChannels.values().stream()
                .anyMatch(existing -> !existing.getId().equals(channelId)
                        && fingerprint.equals(packagerFingerprint(toConfig(existing, "application.yml"))));
        boolean databasePackagerExists = channelRepository.findAll().stream()
                .anyMatch(existing -> !existing.getId().equals(channelId)
                        && fingerprint.equals(packagerFingerprint(existing)));
        if (staticPackagerExists || databasePackagerExists) {
            throw new IllegalStateException("Packager 配置已被其他渠道使用；创建渠道时必须为当前渠道提供独立且确认正确的方言配置");
        }
    }

    private String packagerFingerprint(ChannelConfigDto channel) {
        if (channel == null) {
            return "";
        }
        return String.join("|",
                cleanFingerprintPart(channel.getPackagerType()),
                cleanFingerprintPart(channel.getPackagerConfigMode()),
                cleanFingerprintPart(channel.getPackagerLocation()),
                cleanFingerprintPart(channel.getPackagerFileName()),
                cleanFingerprintPart(channel.getPackagerContent()),
                cleanFingerprintPart(channel.getPackagerClassName()));
    }

    private String cleanFingerprintPart(String value) {
        return value == null ? "" : value.trim();
    }

    private String channelCode(ChannelProperties channel, String source) {
        if ("application.yml".equals(source)) {
            return channelCode(channel.getId());
        }
        return channelCode(firstNonBlank(channel.getName(), channel.getId()));
    }

    private String channelCode(ChannelConfigDto channel) {
        return channelCode(firstNonBlank(channel.getChannelCode(), channel.getName(), channel.getId()));
    }

    private String channelCode(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ISOFieldPackager fieldPackager(ISOBasePackager packager, int id) {
        try {
            return packager.getFieldPackager(id);
        } catch (Exception e) {
            return null;
        }
    }

    private int maxPackedLength(ISOFieldPackager fieldPackager) {
        try {
            return fieldPackager.getMaxPackedLength();
        } catch (Exception e) {
            return 0;
        }
    }
}
