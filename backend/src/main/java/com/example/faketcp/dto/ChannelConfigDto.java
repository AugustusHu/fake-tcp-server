package com.example.faketcp.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class ChannelConfigDto {
    private String id;
    @NotBlank
    private String channelCode;
    private String name;
    private boolean enabled = true;
    private String host = "0.0.0.0";
    @Min(14400)
    @Max(14700)
    private Integer port;
    private String framingType = "BINARY_2";
    private String byteOrder = "BIG_ENDIAN";
    private String lengthIncludes = "PAYLOAD";
    private boolean headerEnabled;
    private int headerLength;
    private String headerResponseMode = "NONE";
    private String headerFixedValueHex;
    private String packagerType = "CLASS";
    private String packagerConfigMode = "CLASS_NAME";
    private String packagerLocation;
    private String packagerFileName;
    private String packagerContent;
    private String packagerClassName;
    private String noMatchResponseCode = "96";
    private String thirdPartyTestIp;
    private Integer thirdPartyTestPort;
    private boolean thirdPartyTlsEnabled;
    private String ctmk1;
    private String ctmk2;
    private Boolean mockTlsEnabled;
    private String mockCtmk1;
    private String mockCtmk2;

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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getFramingType() {
        return framingType;
    }

    public void setFramingType(String framingType) {
        this.framingType = framingType;
    }

    public String getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(String byteOrder) {
        this.byteOrder = byteOrder;
    }

    public String getLengthIncludes() {
        return lengthIncludes;
    }

    public void setLengthIncludes(String lengthIncludes) {
        this.lengthIncludes = lengthIncludes;
    }

    public boolean isHeaderEnabled() {
        return headerEnabled;
    }

    public void setHeaderEnabled(boolean headerEnabled) {
        this.headerEnabled = headerEnabled;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    public void setHeaderLength(int headerLength) {
        this.headerLength = headerLength;
    }

    public String getHeaderResponseMode() {
        return headerResponseMode;
    }

    public void setHeaderResponseMode(String headerResponseMode) {
        this.headerResponseMode = headerResponseMode;
    }

    public String getHeaderFixedValueHex() {
        return headerFixedValueHex;
    }

    public void setHeaderFixedValueHex(String headerFixedValueHex) {
        this.headerFixedValueHex = headerFixedValueHex;
    }

    public String getPackagerType() {
        return packagerType;
    }

    public void setPackagerType(String packagerType) {
        this.packagerType = packagerType;
    }

    public String getPackagerConfigMode() {
        return packagerConfigMode;
    }

    public void setPackagerConfigMode(String packagerConfigMode) {
        this.packagerConfigMode = packagerConfigMode;
    }

    public String getPackagerLocation() {
        return packagerLocation;
    }

    public void setPackagerLocation(String packagerLocation) {
        this.packagerLocation = packagerLocation;
    }

    public String getPackagerFileName() {
        return packagerFileName;
    }

    public void setPackagerFileName(String packagerFileName) {
        this.packagerFileName = packagerFileName;
    }

    public String getPackagerContent() {
        return packagerContent;
    }

    public void setPackagerContent(String packagerContent) {
        this.packagerContent = packagerContent;
    }

    public String getPackagerClassName() {
        return packagerClassName;
    }

    public void setPackagerClassName(String packagerClassName) {
        this.packagerClassName = packagerClassName;
    }

    public String getNoMatchResponseCode() {
        return noMatchResponseCode;
    }

    public void setNoMatchResponseCode(String noMatchResponseCode) {
        this.noMatchResponseCode = noMatchResponseCode;
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

    public String getCtmk1() {
        return ctmk1;
    }

    public void setCtmk1(String ctmk1) {
        this.ctmk1 = ctmk1;
    }

    public String getCtmk2() {
        return ctmk2;
    }

    public void setCtmk2(String ctmk2) {
        this.ctmk2 = ctmk2;
    }

    public Boolean getMockTlsEnabled() {
        return mockTlsEnabled;
    }

    public void setMockTlsEnabled(Boolean mockTlsEnabled) {
        this.mockTlsEnabled = mockTlsEnabled;
    }

    public String getMockCtmk1() {
        return mockCtmk1;
    }

    public void setMockCtmk1(String mockCtmk1) {
        this.mockCtmk1 = mockCtmk1;
    }

    public String getMockCtmk2() {
        return mockCtmk2;
    }

    public void setMockCtmk2(String mockCtmk2) {
        this.mockCtmk2 = mockCtmk2;
    }
}
