package com.example.faketcp.repository;

import com.example.faketcp.dto.DebugCollectionDto;
import com.example.faketcp.dto.DebugRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DebugCollectionRepository {
    private static final TypeReference<List<DebugRequestDto>> REQUESTS_TYPE = new TypeReference<List<DebugRequestDto>>() {};
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DebugCollectionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<DebugCollectionDto> findByEnvironment(long userId, String environmentId) {
        ensureEnvironmentOwned(userId, environmentId);
        return jdbcTemplate.query(
                "SELECT * FROM debug_collection WHERE user_id = ? AND environment_id = ? ORDER BY created_at ASC, id ASC",
                this::map,
                userId,
                environmentId);
    }

    public DebugCollectionDto create(long userId, DebugCollectionDto collection) {
        normalize(userId, collection);
        jdbcTemplate.update(
                "INSERT INTO debug_collection (id, user_id, environment_id, name, requests_json) VALUES (?, ?, ?, ?, ?)",
                collection.getId(),
                userId,
                collection.getEnvironmentId(),
                collection.getName(),
                requestsJson(collection));
        return findOne(userId, collection.getId());
    }

    public DebugCollectionDto update(long userId, String collectionId, DebugCollectionDto collection) {
        collection.setId(collectionId);
        normalize(userId, collection);
        int updated = jdbcTemplate.update(
                "UPDATE debug_collection SET environment_id = ?, name = ?, requests_json = ? WHERE id = ? AND user_id = ?",
                collection.getEnvironmentId(),
                collection.getName(),
                requestsJson(collection),
                collectionId,
                userId);
        if (updated == 0) {
            throw new IllegalArgumentException("Collection not found: " + collectionId);
        }
        return findOne(userId, collectionId);
    }

    public void delete(long userId, String collectionId) {
        jdbcTemplate.update("DELETE FROM debug_collection WHERE id = ? AND user_id = ?", collectionId, userId);
    }

    private DebugCollectionDto findOne(long userId, String collectionId) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM debug_collection WHERE id = ? AND user_id = ?",
                this::map,
                collectionId,
                userId);
    }

    private void normalize(long userId, DebugCollectionDto collection) {
        collection.setUserId(userId);
        if (isBlank(collection.getId())) {
            collection.setId("col_" + UUID.randomUUID().toString().replace("-", ""));
        }
        collection.setEnvironmentId(trim(collection.getEnvironmentId()));
        collection.setName(trim(collection.getName()));
        if (isBlank(collection.getEnvironmentId())) {
            throw new IllegalStateException("Collection 必须绑定 Environment");
        }
        ensureEnvironmentOwned(userId, collection.getEnvironmentId());
        if (isBlank(collection.getName())) {
            collection.setName("Untitled Collection");
        }
        if (collection.getRequests() == null) {
            collection.setRequests(Collections.emptyList());
        }
    }

    private DebugCollectionDto map(ResultSet rs, int rowNum) throws SQLException {
        DebugCollectionDto collection = new DebugCollectionDto();
        collection.setId(rs.getString("id"));
        collection.setUserId(rs.getLong("user_id"));
        collection.setEnvironmentId(rs.getString("environment_id"));
        collection.setName(rs.getString("name"));
        collection.setRequests(parseRequests(rs.getString("requests_json")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        collection.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        collection.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
        return collection;
    }

    private String requestsJson(DebugCollectionDto collection) {
        try {
            return objectMapper.writeValueAsString(collection.getRequests() == null ? Collections.emptyList() : collection.getRequests());
        } catch (Exception e) {
            throw new IllegalStateException("Collection requests 序列化失败: " + e.getMessage(), e);
        }
    }

    private List<DebugRequestDto> parseRequests(String value) {
        if (isBlank(value)) {
            return Collections.emptyList();
        }
        try {
            List<DebugRequestDto> requests = objectMapper.readValue(value, REQUESTS_TYPE);
            return requests == null ? Collections.emptyList() : requests;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void ensureEnvironmentOwned(long userId, String environmentId) {
        if (isBlank(environmentId)) {
            throw new IllegalStateException("Collection 必须绑定 Environment");
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM debug_environment WHERE id = ? AND user_id = ?",
                Integer.class,
                environmentId,
                userId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Environment not found: " + environmentId);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
