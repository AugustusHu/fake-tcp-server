package com.example.faketcp.repository;

import com.example.faketcp.dto.DebugHistoryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DebugHistoryRepository {
    private static final int MAX_HISTORY_PER_ENVIRONMENT = 30;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DebugHistoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<DebugHistoryDto> findRecent(long userId, String environmentId) {
        ensureEnvironmentOwned(userId, environmentId);
        return jdbcTemplate.query(
                "SELECT * FROM debug_history WHERE user_id = ? AND environment_id = ? ORDER BY created_at DESC, id DESC LIMIT ?",
                this::map,
                userId,
                environmentId,
                MAX_HISTORY_PER_ENVIRONMENT);
    }

    public DebugHistoryDto create(long userId, DebugHistoryDto history) {
        normalize(userId, history);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO debug_history "
                            + "(user_id, environment_id, kind, title, capability, success, response_code, request_xml, result_json) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setLong(1, userId);
            ps.setString(2, history.getEnvironmentId());
            ps.setString(3, history.getKind());
            ps.setString(4, history.getTitle());
            ps.setString(5, blankToNull(history.getCapability()));
            ps.setBoolean(6, history.isSuccess());
            ps.setString(7, blankToNull(history.getResponseCode()));
            ps.setString(8, blankToNull(history.getRequestXml()));
            ps.setString(9, resultJson(history));
            return ps;
        }, keyHolder);
        trim(userId, history.getEnvironmentId());
        return findOne(keyHolder.getKey().longValue());
    }

    public void delete(long userId, String environmentId, long historyId) {
        ensureEnvironmentOwned(userId, environmentId);
        jdbcTemplate.update(
                "DELETE FROM debug_history WHERE id = ? AND user_id = ? AND environment_id = ?",
                historyId,
                userId,
                environmentId);
    }

    public void clear(long userId, String environmentId) {
        ensureEnvironmentOwned(userId, environmentId);
        jdbcTemplate.update("DELETE FROM debug_history WHERE user_id = ? AND environment_id = ?", userId, environmentId);
    }

    private DebugHistoryDto findOne(long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM debug_history WHERE id = ?", this::map, id);
    }

    private void normalize(long userId, DebugHistoryDto history) {
        history.setUserId(userId);
        history.setEnvironmentId(trim(history.getEnvironmentId()));
        history.setKind(isBlank(history.getKind()) ? "send" : history.getKind().trim());
        history.setTitle(isBlank(history.getTitle()) ? "POS Debug" : history.getTitle().trim());
        if (isBlank(history.getEnvironmentId())) {
            throw new IllegalStateException("History 必须绑定 Environment");
        }
        ensureEnvironmentOwned(userId, history.getEnvironmentId());
    }

    private void trim(long userId, String environmentId) {
        jdbcTemplate.update(
                "DELETE FROM debug_history "
                        + "WHERE user_id = ? AND environment_id = ? AND id NOT IN ("
                        + "  SELECT id FROM ("
                        + "    SELECT id FROM debug_history WHERE user_id = ? AND environment_id = ? ORDER BY created_at DESC, id DESC LIMIT ?"
                        + "  ) recent_history"
                        + ")",
                userId,
                environmentId,
                userId,
                environmentId,
                MAX_HISTORY_PER_ENVIRONMENT);
    }

    private DebugHistoryDto map(ResultSet rs, int rowNum) throws SQLException {
        DebugHistoryDto history = new DebugHistoryDto();
        history.setId(rs.getLong("id"));
        history.setUserId(rs.getLong("user_id"));
        history.setEnvironmentId(rs.getString("environment_id"));
        history.setKind(rs.getString("kind"));
        history.setTitle(rs.getString("title"));
        history.setCapability(rs.getString("capability"));
        history.setSuccess(rs.getBoolean("success"));
        history.setResponseCode(rs.getString("response_code"));
        history.setRequestXml(rs.getString("request_xml"));
        history.setResult(parseResult(rs.getString("result_json")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        history.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        return history;
    }

    private String resultJson(DebugHistoryDto history) {
        if (history.getResult() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(history.getResult());
        } catch (Exception e) {
            throw new IllegalStateException("History result 序列化失败: " + e.getMessage(), e);
        }
    }

    private JsonNode parseResult(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void ensureEnvironmentOwned(long userId, String environmentId) {
        if (isBlank(environmentId)) {
            throw new IllegalStateException("History 必须绑定 Environment");
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

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
