package com.example.faketcp.repository;

import com.example.faketcp.dto.DebugEnvironmentDto;
import com.example.faketcp.dto.DebugEnvironmentVariableDto;
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
public class DebugEnvironmentRepository {
    private static final TypeReference<List<DebugEnvironmentVariableDto>> VARIABLES_TYPE = new TypeReference<List<DebugEnvironmentVariableDto>>() {};
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DebugEnvironmentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<DebugEnvironmentDto> findByUser(long userId) {
        return jdbcTemplate.query(
                "SELECT * FROM debug_environment WHERE user_id = ? ORDER BY created_at ASC, id ASC",
                this::map,
                userId);
    }

    public DebugEnvironmentDto findOne(long userId, String environmentId) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM debug_environment WHERE id = ? AND user_id = ?",
                this::map,
                environmentId,
                userId);
    }

    public DebugEnvironmentDto create(long userId, DebugEnvironmentDto environment) {
        if (isBlank(environment.getId())) {
            environment.setId("env_" + UUID.randomUUID().toString().replace("-", ""));
        }
        environment.setUserId(userId);
        normalize(environment);
        jdbcTemplate.update(
                "INSERT INTO debug_environment "
                        + "(id, user_id, channel_id, name, target_ip, target_port, tmk_plain, tpk_plain, tsk_plain, "
                        + "mac_field, mac_algorithm, pin_algorithm, test_tid, test_sn, test_pan, test_de14, test_de52, "
                        + "test_de42, test_de18, test_de43, test_de49, variables_json) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                environment.getId(),
                userId,
                environment.getChannelId(),
                environment.getName(),
                blankToNull(environment.getTargetIp()),
                environment.getTargetPort(),
                blankToNull(environment.getTmkPlain()),
                blankToNull(environment.getTpkPlain()),
                blankToNull(environment.getTskPlain()),
                blankToNull(environment.getMacField()),
                environment.getMacAlgorithm(),
                environment.getPinAlgorithm(),
                blankToNull(environment.getTestTid()),
                blankToNull(environment.getTestSn()),
                blankToNull(environment.getTestPan()),
                blankToNull(environment.getTestDe14()),
                blankToNull(environment.getTestDe52()),
                blankToNull(environment.getTestDe42()),
                blankToNull(environment.getTestDe18()),
                blankToNull(environment.getTestDe43()),
                blankToNull(environment.getTestDe49()),
                variablesJson(environment));
        return findOne(userId, environment.getId());
    }

    public DebugEnvironmentDto update(long userId, String environmentId, DebugEnvironmentDto environment) {
        environment.setId(environmentId);
        environment.setUserId(userId);
        normalize(environment);
        int updated = jdbcTemplate.update(
                "UPDATE debug_environment SET channel_id = ?, name = ?, target_ip = ?, target_port = ?, "
                        + "tmk_plain = ?, tpk_plain = ?, tsk_plain = ?, mac_field = ?, mac_algorithm = ?, pin_algorithm = ?, "
                        + "test_tid = ?, test_sn = ?, test_pan = ?, test_de14 = ?, test_de52 = ?, test_de42 = ?, "
                        + "test_de18 = ?, test_de43 = ?, test_de49 = ?, variables_json = ? "
                        + "WHERE id = ? AND user_id = ?",
                environment.getChannelId(),
                environment.getName(),
                blankToNull(environment.getTargetIp()),
                environment.getTargetPort(),
                blankToNull(environment.getTmkPlain()),
                blankToNull(environment.getTpkPlain()),
                blankToNull(environment.getTskPlain()),
                blankToNull(environment.getMacField()),
                environment.getMacAlgorithm(),
                environment.getPinAlgorithm(),
                blankToNull(environment.getTestTid()),
                blankToNull(environment.getTestSn()),
                blankToNull(environment.getTestPan()),
                blankToNull(environment.getTestDe14()),
                blankToNull(environment.getTestDe52()),
                blankToNull(environment.getTestDe42()),
                blankToNull(environment.getTestDe18()),
                blankToNull(environment.getTestDe43()),
                blankToNull(environment.getTestDe49()),
                variablesJson(environment),
                environmentId,
                userId);
        if (updated == 0) {
            throw new IllegalArgumentException("Environment not found: " + environmentId);
        }
        return findOne(userId, environmentId);
    }

    public void delete(long userId, String environmentId) {
        jdbcTemplate.update("DELETE FROM debug_environment WHERE id = ? AND user_id = ?", environmentId, userId);
    }

    private void normalize(DebugEnvironmentDto environment) {
        environment.setChannelId(trim(environment.getChannelId()));
        environment.setName(trim(environment.getName()));
        if (isBlank(environment.getChannelId())) {
            throw new IllegalStateException("Environment 必须选择协议渠道");
        }
        if (isBlank(environment.getName())) {
            throw new IllegalStateException("Environment 名称不能为空");
        }
        if (environment.getTargetPort() != null && (environment.getTargetPort() < 1 || environment.getTargetPort() > 65535)) {
            throw new IllegalStateException("目标 Port 必须在 1 到 65535 之间");
        }
        environment.setMacAlgorithm(isBlank(environment.getMacAlgorithm()) ? "ANSI_X9_19" : environment.getMacAlgorithm().trim().toUpperCase());
        environment.setPinAlgorithm(isBlank(environment.getPinAlgorithm()) ? "NONE" : environment.getPinAlgorithm().trim().toUpperCase());
    }

    private DebugEnvironmentDto map(ResultSet rs, int rowNum) throws SQLException {
        DebugEnvironmentDto environment = new DebugEnvironmentDto();
        environment.setId(rs.getString("id"));
        environment.setUserId(rs.getLong("user_id"));
        environment.setChannelId(rs.getString("channel_id"));
        environment.setName(rs.getString("name"));
        environment.setTargetIp(rs.getString("target_ip"));
        int targetPort = rs.getInt("target_port");
        environment.setTargetPort(rs.wasNull() ? null : targetPort);
        environment.setTmkPlain(rs.getString("tmk_plain"));
        environment.setTpkPlain(rs.getString("tpk_plain"));
        environment.setTskPlain(rs.getString("tsk_plain"));
        environment.setMacField(rs.getString("mac_field"));
        environment.setMacAlgorithm(rs.getString("mac_algorithm"));
        environment.setPinAlgorithm(rs.getString("pin_algorithm"));
        environment.setTestTid(rs.getString("test_tid"));
        environment.setTestSn(rs.getString("test_sn"));
        environment.setTestPan(rs.getString("test_pan"));
        environment.setTestDe14(rs.getString("test_de14"));
        environment.setTestDe52(rs.getString("test_de52"));
        environment.setTestDe42(rs.getString("test_de42"));
        environment.setTestDe18(rs.getString("test_de18"));
        environment.setTestDe43(rs.getString("test_de43"));
        environment.setTestDe49(rs.getString("test_de49"));
        environment.setVariables(parseVariables(rs.getString("variables_json")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        environment.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        environment.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
        return environment;
    }

    private String variablesJson(DebugEnvironmentDto environment) {
        try {
            return objectMapper.writeValueAsString(environment.getVariables() == null ? Collections.emptyList() : environment.getVariables());
        } catch (Exception e) {
            throw new IllegalStateException("Environment variables 序列化失败: " + e.getMessage(), e);
        }
    }

    private List<DebugEnvironmentVariableDto> parseVariables(String value) {
        if (isBlank(value)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(value, VARIABLES_TYPE);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
