package com.example.faketcp.dto;

public class WireParseResponseDto {
    private String responseXml;
    private String payloadHex;
    private String isoHex;

    public String getResponseXml() {
        return responseXml;
    }

    public void setResponseXml(String responseXml) {
        this.responseXml = responseXml;
    }

    public String getPayloadHex() {
        return payloadHex;
    }

    public void setPayloadHex(String payloadHex) {
        this.payloadHex = payloadHex;
    }

    public String getIsoHex() {
        return isoHex;
    }

    public void setIsoHex(String isoHex) {
        this.isoHex = isoHex;
    }
}
