package com.example.faketcp.dto;

import java.util.ArrayList;
import java.util.List;

public class ChannelDto {
    private String id;
    private String channelCode;
    private String name;
    private boolean enabled;
    private String host;
    private int port;
    private String framing;
    private String packager;
    private String mockAccessHost;
    private String thirdPartyTestIp;
    private Integer thirdPartyTestPort;
    private boolean thirdPartyTlsEnabled;
    private Boolean mockTlsEnabled;
    private String source;
    private boolean restartRequired;
    private List<String> restartReasons = new ArrayList<>();
    private List<String> restartRequiredFields = new ArrayList<>();
    private List<String> hotEffectiveFields = new ArrayList<>();
    private ChannelConfigDto config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFraming() {
        return framing;
    }

    public void setFraming(String framing) {
        this.framing = framing;
    }

    public String getPackager() {
        return packager;
    }

    public void setPackager(String packager) {
        this.packager = packager;
    }

    public String getMockAccessHost() {
        return mockAccessHost;
    }

    public void setMockAccessHost(String mockAccessHost) {
        this.mockAccessHost = mockAccessHost;
    }

    public String getThirdPartyTestIp() {
        return thirdPartyTestIp;
    }

    public void setThirdPartyTestIp(String thirdPartyTestIp) {
        this.thirdPartyTestIp = thirdPartyTestIp;
    }

    public Integer getThirdPartyTestPort() {
        return thirdPartyTestPort;
    }

    public void setThirdPartyTestPort(Integer thirdPartyTestPort) {
        this.thirdPartyTestPort = thirdPartyTestPort;
    }

    public boolean isThirdPartyTlsEnabled() {
        return thirdPartyTlsEnabled;
    }

    public void setThirdPartyTlsEnabled(boolean thirdPartyTlsEnabled) {
        this.thirdPartyTlsEnabled = thirdPartyTlsEnabled;
    }

    public Boolean getMockTlsEnabled() {
        return mockTlsEnabled;
    }

    public void setMockTlsEnabled(Boolean mockTlsEnabled) {
        this.mockTlsEnabled = mockTlsEnabled;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isRestartRequired() {
        return restartRequired;
    }

    public void setRestartRequired(boolean restartRequired) {
        this.restartRequired = restartRequired;
    }

    public List<String> getRestartReasons() {
        return restartReasons;
    }

    public void setRestartReasons(List<String> restartReasons) {
        this.restartReasons = restartReasons;
    }

    public List<String> getRestartRequiredFields() {
        return restartRequiredFields;
    }

    public void setRestartRequiredFields(List<String> restartRequiredFields) {
        this.restartRequiredFields = restartRequiredFields;
    }

    public List<String> getHotEffectiveFields() {
        return hotEffectiveFields;
    }

    public void setHotEffectiveFields(List<String> hotEffectiveFields) {
        this.hotEffectiveFields = hotEffectiveFields;
    }

    public ChannelConfigDto getConfig() {
        return config;
    }

    public void setConfig(ChannelConfigDto config) {
        this.config = config;
    }
}
