package com.example.faketcp.dto;

import java.time.Instant;
import java.util.Map;

public class RequestLogDto {
    private Long id;
    private String channelId;
    private String remoteAddress;
    private String mti;
    private String processingCode;
    private Long matchedRuleId;
    private String matchedRuleName;
    private String actionType;
    private String responseCode;
    private Long durationMs;
    private String requestHex;
    private String responseHex;
    private Map<String, String> requestFields;
    private Map<String, String> responseFields;
    private String errorMessage;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getMti() {
        return mti;
    }

    public void setMti(String mti) {
        this.mti = mti;
    }

    public String getProcessingCode() {
        return processingCode;
    }

    public void setProcessingCode(String processingCode) {
        this.processingCode = processingCode;
    }

    public Long getMatchedRuleId() {
        return matchedRuleId;
    }

    public void setMatchedRuleId(Long matchedRuleId) {
        this.matchedRuleId = matchedRuleId;
    }

    public String getMatchedRuleName() {
        return matchedRuleName;
    }

    public void setMatchedRuleName(String matchedRuleName) {
        this.matchedRuleName = matchedRuleName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
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

    public Map<String, String> getRequestFields() {
        return requestFields;
    }

    public void setRequestFields(Map<String, String> requestFields) {
        this.requestFields = requestFields;
    }

    public Map<String, String> getResponseFields() {
        return responseFields;
    }

    public void setResponseFields(Map<String, String> responseFields) {
        this.responseFields = responseFields;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
