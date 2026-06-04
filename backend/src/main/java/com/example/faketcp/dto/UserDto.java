package com.example.faketcp.dto;

import java.time.Instant;

public class UserDto {
    private Long id;
    private String username;
    private String displayName;
    private boolean admin;
    private String lastChannelId;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getLastChannelId() {
        return lastChannelId;
    }

    public void setLastChannelId(String lastChannelId) {
        this.lastChannelId = lastChannelId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
