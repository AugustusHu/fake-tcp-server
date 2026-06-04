package com.example.faketcp.dto;

import java.util.ArrayList;
import java.util.List;

public class PosDebugStepDto {
    private String capability;
    private String label;
    private String targetIp;
    private Integer targetPort;
    private String packager;
    private String requestXml;
    private String signedRequestXml;
    private String requestHex;
    private String responseHex;
    private String responseXml;
    private String responseCode;
    private boolean macRequired;
    private String macField;
    private String macValue;
    private Boolean responseMacValid;
    private long durationMs;
    private boolean success;
    private String errorMessage;
    private List<String> logs = new ArrayList<>();

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getPackager() {
        return packager;
    }

    public void setPackager(String packager) {
        this.packager = packager;
    }

    public String getRequestXml() {
        return requestXml;
    }

    public void setRequestXml(String requestXml) {
        this.requestXml = requestXml;
    }

    public String getSignedRequestXml() {
        return signedRequestXml;
    }

    public void setSignedRequestXml(String signedRequestXml) {
        this.signedRequestXml = signedRequestXml;
    }

    public String getRequestHex() {
        return requestHex;
    }

    public void setRequestHex(String requestHex) {
        this.requestHex = requestHex;
    }

    public String getResponseHex() {
        return responseHex;
    }

    public void setResponseHex(String responseHex) {
        this.responseHex = responseHex;
    }

    public String getResponseXml() {
        return responseXml;
    }

    public void setResponseXml(String responseXml) {
        this.responseXml = responseXml;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
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

    public Boolean getResponseMacValid() {
        return responseMacValid;
    }

    public void setResponseMacValid(Boolean responseMacValid) {
        this.responseMacValid = responseMacValid;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }
}
