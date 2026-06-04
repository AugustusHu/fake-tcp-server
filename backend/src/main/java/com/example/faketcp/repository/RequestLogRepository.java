package com.example.faketcp.repository;

import com.example.faketcp.dto.RequestLogDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RequestLogRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public RequestLogRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(RequestLogDto log) {
        jdbcTemplate.update(
                "INSERT INTO request_log (channel_id, remote_address, mti, processing_code, matched_rule_id, matched_rule_name, "
                        + "action_type, response_code, duration_ms, request_hex, response_hex, request_fields_json, response_fields_json, error_message) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                log.getChannelId(),
                log.getRemoteAddress(),
                log.getMti(),
                log.getProcessingCode(),
                log.getMatchedRuleId(),
                log.getMatchedRuleName(),
                log.getActionType(),
                log.getResponseCode(),
                log.getDurationMs(),
                log.getRequestHex(),
                log.getResponseHex(),
                write(log.getRequestFields()),
                write(log.getResponseFields()),
                log.getErrorMessage());
    }

    public List<RequestLogDto> findRecent(String channelId, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM request_log WHERE channel_id = ? ORDER BY created_at DESC LIMIT ?",
                this::mapLog,
                channelId,
                limit);
    }

    public RequestLogDto findById(String channelId, long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM request_log WHERE channel_id = ? AND id = ?",
                this::mapLog,
                channelId,
                id);
    }

    private RequestLogDto mapLog(ResultSet rs, int rowNum) throws SQLException {
        RequestLogDto log = new RequestLogDto();
        log.setId(rs.getLong("id"));
        log.setChannelId(rs.getString("channel_id"));
        log.setRemoteAddress(rs.getString("remote_address"));
        log.setMti(rs.getString("mti"));
        log.setProcessingCode(rs.getString("processing_code"));
        long matchedRuleId = rs.getLong("matched_rule_id");
        log.setMatchedRuleId(rs.wasNull() ? null : matchedRuleId);
        log.setMatchedRuleName(rs.getString("matched_rule_name"));
        log.setActionType(rs.getString("action_type"));
        log.setResponseCode(rs.getString("response_code"));
        long durationMs = rs.getLong("duration_ms");
        log.setDurationMs(rs.wasNull() ? null : durationMs);
        log.setRequestHex(rs.getString("request_hex"));
        log.setResponseHex(rs.getString("response_hex"));
        log.setRequestFields(readMap(rs.getString("request_fields_json")));
        log.setResponseFields(readMap(rs.getString("response_fields_json")));
        log.setErrorMessage(rs.getString("error_message"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        log.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        return log;
    }

    private String write(Map<String, String> fields) {
        try {
            return objectMapper.writeValueAsString(fields == null ? new LinkedHashMap<>() : fields);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize request log JSON", e);
        }
    }

    private Map<String, String> readMap(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse request log JSON", e);
        }
    }
}
