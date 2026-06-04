package com.example.faketcp.repository;

import com.example.faketcp.dto.ActionDto;
import com.example.faketcp.dto.ConditionDto;
import com.example.faketcp.dto.ResponseDto;
import com.example.faketcp.dto.RuleCapability;
import com.example.faketcp.dto.RuleDto;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class RuleRepository {
    private static final String RULE_SELECT =
            "SELECT r.*, u.username AS owner_username FROM mock_rule r LEFT JOIN app_user u ON r.owner_user_id = u.id";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public RuleRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<RuleDto> findByChannel(String channelId) {
        return jdbcTemplate.query(
                RULE_SELECT + " WHERE r.channel_id = ? ORDER BY r.priority DESC, r.id ASC",
                this::mapRule,
                channelId);
    }

    public List<RuleDto> findByChannel(String channelId, long ownerUserId) {
        return jdbcTemplate.query(
                RULE_SELECT + " WHERE r.channel_id = ? AND r.owner_user_id = ? ORDER BY r.priority DESC, r.id ASC",
                this::mapRule,
                channelId,
                ownerUserId);
    }

    public List<RuleDto> findByChannel(String channelId, RuleCapability capability) {
        return jdbcTemplate.query(
                RULE_SELECT + " WHERE r.channel_id = ? AND r.capability = ? ORDER BY r.priority DESC, r.id ASC",
                this::mapRule,
                channelId,
                capability.name());
    }

    public List<RuleDto> findByChannel(String channelId, RuleCapability capability, long ownerUserId) {
        return jdbcTemplate.query(
                RULE_SELECT + " WHERE r.channel_id = ? AND r.capability = ? AND r.owner_user_id = ? ORDER BY r.priority DESC, r.id ASC",
                this::mapRule,
                channelId,
                capability.name(),
                ownerUserId);
    }

    public List<RuleDto> findEnabledByChannel(String channelId) {
        return jdbcTemplate.query(
                RULE_SELECT + " WHERE r.channel_id = ? AND r.enabled = TRUE ORDER BY r.priority DESC, r.id ASC",
                this::mapRule,
                channelId);
    }

    public List<RuleDto> findEnabledByChannel(String channelId, long ownerUserId) {
        return jdbcTemplate.query(
                RULE_SELECT + " WHERE r.channel_id = ? AND r.enabled = TRUE AND r.owner_user_id = ? ORDER BY r.priority DESC, r.id ASC",
                this::mapRule,
                channelId,
                ownerUserId);
    }

    public RuleDto findById(String channelId, long id) {
        return jdbcTemplate.queryForObject(
                RULE_SELECT + " WHERE r.channel_id = ? AND r.id = ?",
                this::mapRule,
                channelId,
                id);
    }

    public RuleDto findById(String channelId, long id, long ownerUserId) {
        return jdbcTemplate.queryForObject(
                RULE_SELECT + " WHERE r.channel_id = ? AND r.id = ? AND r.owner_user_id = ?",
                this::mapRule,
                channelId,
                id,
                ownerUserId);
    }

    public RuleDto create(String channelId, RuleDto rule) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO mock_rule (channel_id, owner_user_id, name, description, enabled, public_rule, capability, priority, match_mode, system_conditions_json, conditions_json, action_json, response_json) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, channelId);
            ps.setObject(2, rule.getOwnerUserId() == null ? 1L : rule.getOwnerUserId());
            ps.setString(3, rule.getName());
            ps.setString(4, rule.getDescription());
            ps.setBoolean(5, rule.isEnabled());
            ps.setBoolean(6, rule.isPublicRule());
            ps.setString(7, rule.getCapability().name());
            ps.setInt(8, rule.getPriority());
            ps.setString(9, rule.getMatchMode().name());
            ps.setString(10, write(rule.getSystemConditions()));
            ps.setString(11, write(rule.getConditions()));
            ps.setString(12, write(rule.getAction()));
            ps.setString(13, write(rule.getResponse()));
            return ps;
        }, keyHolder);
        return findById(channelId, keyHolder.getKey().longValue());
    }

    public RuleDto update(String channelId, long id, long ownerUserId, RuleDto rule) {
        int updated = jdbcTemplate.update(
                "UPDATE mock_rule SET name = ?, description = ?, enabled = ?, public_rule = ?, capability = ?, priority = ?, match_mode = ?, "
                        + "system_conditions_json = ?, conditions_json = ?, action_json = ?, response_json = ? "
                        + "WHERE channel_id = ? AND id = ? AND owner_user_id = ?",
                rule.getName(),
                rule.getDescription(),
                rule.isEnabled(),
                rule.isPublicRule(),
                rule.getCapability().name(),
                rule.getPriority(),
                rule.getMatchMode().name(),
                write(rule.getSystemConditions()),
                write(rule.getConditions()),
                write(rule.getAction()),
                write(rule.getResponse()),
                channelId,
                id,
                ownerUserId);
        requireAffected(updated, "Rule not found or not owned by current user: " + id);
        return findById(channelId, id, ownerUserId);
    }

    public void setEnabled(String channelId, long id, long ownerUserId, boolean enabled) {
        int updated = jdbcTemplate.update(
                "UPDATE mock_rule SET enabled = ? WHERE channel_id = ? AND id = ? AND owner_user_id = ?",
                enabled,
                channelId,
                id,
                ownerUserId);
        requireAffected(updated, "Rule not found or not owned by current user: " + id);
    }

    public void setPublicRule(String channelId, long id, long ownerUserId, boolean publicRule) {
        int updated = jdbcTemplate.update(
                "UPDATE mock_rule SET public_rule = ? WHERE channel_id = ? AND id = ? AND owner_user_id = ?",
                publicRule,
                channelId,
                id,
                ownerUserId);
        requireAffected(updated, "Rule not found or not owned by current user: " + id);
    }

    public List<RuleDto> findPublicRules() {
        return jdbcTemplate.query(
                RULE_SELECT + " WHERE r.public_rule = TRUE ORDER BY r.updated_at DESC, r.id DESC",
                this::mapRule);
    }

    public void delete(String channelId, long id, long ownerUserId) {
        int updated = jdbcTemplate.update(
                "DELETE FROM mock_rule WHERE channel_id = ? AND id = ? AND owner_user_id = ?",
                channelId,
                id,
                ownerUserId);
        requireAffected(updated, "Rule not found or not owned by current user: " + id);
    }

    private void requireAffected(int rows, String message) {
        if (rows <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private RuleDto mapRule(ResultSet rs, int rowNum) throws SQLException {
        RuleDto rule = new RuleDto();
        rule.setId(rs.getLong("id"));
        rule.setChannelId(rs.getString("channel_id"));
        long ownerUserId = rs.getLong("owner_user_id");
        rule.setOwnerUserId(rs.wasNull() ? 1L : ownerUserId);
        rule.setOwnerUsername(rs.getString("owner_username"));
        rule.setName(rs.getString("name"));
        rule.setDescription(rs.getString("description"));
        rule.setEnabled(rs.getBoolean("enabled"));
        rule.setPublicRule(rs.getBoolean("public_rule"));
        rule.setCapability(readCapability(rs.getString("capability")));
        rule.setPriority(rs.getInt("priority"));
        rule.setMatchMode(RuleDto.MatchMode.valueOf(rs.getString("match_mode")));
        String systemConditionsJson = rs.getString("system_conditions_json");
        if (systemConditionsJson != null) {
            rule.setSystemConditions(read(systemConditionsJson, new TypeReference<List<ConditionDto>>() {}));
        }
        rule.setConditions(read(rs.getString("conditions_json"), new TypeReference<List<ConditionDto>>() {}));
        rule.setAction(read(rs.getString("action_json"), ActionDto.class));
        rule.setResponse(read(rs.getString("response_json"), ResponseDto.class));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        rule.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        rule.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
        return rule;
    }

    private RuleCapability readCapability(String value) {
        if (value == null || value.trim().isEmpty()) {
            return RuleCapability.DEBIT;
        }
        if ("TID_INIT".equals(value.trim())) {
            return RuleCapability.TID_INIT_9A;
        }
        if ("TID_INIT_9C".equals(value.trim())) {
            return RuleCapability.TID_INIT_9B;
        }
        try {
            return RuleCapability.valueOf(value);
        } catch (IllegalArgumentException e) {
            return RuleCapability.DEBIT;
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize rule JSON", e);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse rule JSON", e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse rule JSON", e);
        }
    }
}
