package com.example.faketcp.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DebugCollectionDto {
    private String id;
    private Long userId;
    private String environmentId;
    private String name;
    private List<DebugRequestDto> requests = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DebugRequestDto> getRequests() {
        return requests;
    }

    public void setRequests(List<DebugRequestDto> requests) {
        this.requests = requests;
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
}
