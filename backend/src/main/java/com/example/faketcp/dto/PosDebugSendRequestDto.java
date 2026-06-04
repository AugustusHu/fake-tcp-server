package com.example.faketcp.dto;

public class PosDebugSendRequestDto {
    private String environmentId;
    private String capability;
    private String requestXml;
    private String macAlgorithm;
    private String pinAlgorithm = "NONE";
    private String pan;
    private String pin;
    private String de52;
    private boolean saveTidConfig;
    private String targetIp;
    private Integer targetPort;
    private String tpkPlain;
    private String tskPlain;
    private String macField;

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getRequestXml() {
        return requestXml;
    }

    public void setRequestXml(String requestXml) {
        this.requestXml = requestXml;
    }

    public String getMacAlgorithm() {
        return macAlgorithm;
    }

    public void setMacAlgorithm(String macAlgorithm) {
        this.macAlgorithm = macAlgorithm;
    }

    public String getPinAlgorithm() {
        return pinAlgorithm;
    }

    public void setPinAlgorithm(String pinAlgorithm) {
        this.pinAlgorithm = pinAlgorithm;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getDe52() {
        return de52;
    }

    public void setDe52(String de52) {
        this.de52 = de52;
    }

    public boolean isSaveTidConfig() {
        return saveTidConfig;
    }

    public void setSaveTidConfig(boolean saveTidConfig) {
        this.saveTidConfig = saveTidConfig;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }

    public String getTpkPlain() {
        return tpkPlain;
    }

    public void setTpkPlain(String tpkPlain) {
        this.tpkPlain = tpkPlain;
    }

    public String getTskPlain() {
        return tskPlain;
    }

    public void setTskPlain(String tskPlain) {
        this.tskPlain = tskPlain;
    }

    public String getMacField() {
        return macField;
    }

    public void setMacField(String macField) {
        this.macField = macField;
    }
}
