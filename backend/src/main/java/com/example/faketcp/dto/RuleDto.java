package com.example.faketcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RuleDto {
    private Long id;
    private String channelId;
    private Long ownerUserId = 1L;
    private String ownerUsername;
    private String name;
    private String description;
    private boolean enabled = true;
    private boolean publicRule;
    private RuleCapability capability = RuleCapability.DEBIT;
    private int priority;
    private MatchMode matchMode = MatchMode.ALL;
    private List<ConditionDto> systemConditions = new ArrayList<>();
    private List<ConditionDto> conditions = new ArrayList<>();
    private ActionDto action = new ActionDto();
    private ResponseDto response = new ResponseDto();
    private Instant createdAt;
    private Instant updatedAt;

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

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPublicRule() {
        return publicRule;
    }

    public void setPublicRule(boolean publicRule) {
        this.publicRule = publicRule;
    }

    public RuleCapability getCapability() {
        return capability;
    }

    public void setCapability(RuleCapability capability) {
        this.capability = capability;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public MatchMode getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(MatchMode matchMode) {
        this.matchMode = matchMode;
    }

    public List<ConditionDto> getSystemConditions() {
        return systemConditions;
    }

    public void setSystemConditions(List<ConditionDto> systemConditions) {
        this.systemConditions = systemConditions;
    }

    public List<ConditionDto> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionDto> conditions) {
        this.conditions = conditions;
    }

    public ActionDto getAction() {
        return action;
    }

    public void setAction(ActionDto action) {
        this.action = action;
    }

    public ResponseDto getResponse() {
        return response;
    }

    public void setResponse(ResponseDto response) {
        this.response = response;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonIgnore
    public boolean isRestartRequired() {
        return false;
    }

    @JsonIgnore
    public List<String> getRestartRequiredFields() {
        return Collections.emptyList();
    }

    @JsonIgnore
    public List<String> getHotEffectiveFields() {
        return Arrays.asList(
                "name",
                "description",
                "enabled",
                "publicRule",
                "capability",
                "priority",
                "matchMode",
                "systemConditions",
                "conditions",
                "action",
                "response");
    }

    public enum MatchMode {
        ALL,
        ANY
    }
}
