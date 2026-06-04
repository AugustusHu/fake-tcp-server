package com.example.faketcp.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestGroupDto {
    private Long id;
    private Long userId;
    private String channelId;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TestCaseDto> cases = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<TestCaseDto> getCases() { return cases; }
    public void setCases(List<TestCaseDto> cases) { this.cases = cases; }
}
