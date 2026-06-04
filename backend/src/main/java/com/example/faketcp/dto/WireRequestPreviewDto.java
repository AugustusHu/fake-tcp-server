package com.example.faketcp.dto;

public class WireRequestPreviewDto {
    private String requestXml;
    private String isoHex;
    private String payloadHex;
    private String frameHex;
    private boolean macRequired;
    private String macField;
    private String macValue;
    private String targetHost;
    private int targetPort;
    private String sendHint;

    public String getRequestXml() {
        return requestXml;
    }

    public void setRequestXml(String requestXml) {
        this.requestXml = requestXml;
    }

    public String getIsoHex() {
        return isoHex;
    }

    public void setIsoHex(String isoHex) {
        this.isoHex = isoHex;
    }

    public String getPayloadHex() {
        return payloadHex;
    }

    public void setPayloadHex(String payloadHex) {
        this.payloadHex = payloadHex;
    }

    public String getFrameHex() {
        return frameHex;
    }

    public void setFrameHex(String frameHex) {
        this.frameHex = frameHex;
    }

    public boolean isMacRequired() {
        return macRequired;
    }

    public void setMacRequired(boolean macRequired) {
        this.macRequired = macRequired;
    }

    public String getMacField() {
        return macField;
    }

    public void setMacField(String macField) {
        this.macField = macField;
    }

    public String getMacValue() {
        return macValue;
    }

    public void setMacValue(String macValue) {
        this.macValue = macValue;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public String getSendHint() {
        return sendHint;
    }

    public void setSendHint(String sendHint) {
        this.sendHint = sendHint;
    }
}
