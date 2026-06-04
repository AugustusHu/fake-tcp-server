package com.example.faketcp.repository;

import com.example.faketcp.dto.TestCaseDto;
import com.example.faketcp.dto.TestGroupDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TestGroupRepository {
    private final JdbcTemplate jdbcTemplate;

    public TestGroupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TestGroupDto> findGroups(long userId, String channelId) {
        List<TestGroupDto> groups = jdbcTemplate.query(
                "SELECT * FROM test_group WHERE user_id = ? AND channel_id = ? ORDER BY name ASC",
                this::mapGroup,
                userId,
                channelId);
        groups.forEach(group -> group.setCases(findCases(group.getId())));
        return groups;
    }

    public TestGroupDto createGroup(long userId, String channelId, TestGroupDto group) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO test_group (user_id, channel_id, name) VALUES (?, ?, ?)",
                    new String[]{"id"});
            ps.setLong(1, userId);
            ps.setString(2, channelId);
            ps.setString(3, group.getName());
            return ps;
        }, keyHolder);
        return findGroup(keyHolder.getKey().longValue());
    }

    public TestCaseDto createCase(long groupId, TestCaseDto testCase) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO test_case (group_id, name, enabled, request_xml) VALUES (?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setLong(1, groupId);
            ps.setString(2, testCase.getName());
            ps.setBoolean(3, testCase.isEnabled());
            ps.setString(4, testCase.getRequestXml());
            return ps;
        }, keyHolder);
        return findCase(keyHolder.getKey().longValue());
    }

    public void deleteGroup(long groupId) {
        jdbcTemplate.update("DELETE FROM test_group WHERE id = ?", groupId);
    }

    public void deleteGroup(long groupId, long userId, String channelId) {
        jdbcTemplate.update(
                "DELETE FROM test_group WHERE id = ? AND user_id = ? AND channel_id = ?",
                groupId,
                userId,
                channelId);
    }

    public void deleteCase(long caseId) {
        jdbcTemplate.update("DELETE FROM test_case WHERE id = ?", caseId);
    }

    public void deleteCase(long caseId, long groupId, long userId, String channelId) {
        jdbcTemplate.update(
                "DELETE tc FROM test_case tc JOIN test_group tg ON tc.group_id = tg.id "
                        + "WHERE tc.id = ? AND tc.group_id = ? AND tg.user_id = ? AND tg.channel_id = ?",
                caseId,
                groupId,
                userId,
                channelId);
    }

    public TestGroupDto findGroup(long groupId) {
        TestGroupDto group = jdbcTemplate.queryForObject("SELECT * FROM test_group WHERE id = ?", this::mapGroup, groupId);
        group.setCases(findCases(groupId));
        return group;
    }

    public TestGroupDto findGroup(long groupId, long userId, String channelId) {
        TestGroupDto group = jdbcTemplate.queryForObject(
                "SELECT * FROM test_group WHERE id = ? AND user_id = ? AND channel_id = ?",
                this::mapGroup,
                groupId,
                userId,
                channelId);
        group.setCases(findCases(groupId, userId, channelId));
        return group;
    }

    public List<TestCaseDto> findCases(long groupId) {
        return jdbcTemplate.query("SELECT * FROM test_case WHERE group_id = ? ORDER BY id ASC", this::mapCase, groupId);
    }

    public List<TestCaseDto> findCases(long groupId, long userId, String channelId) {
        return jdbcTemplate.query(
                "SELECT tc.* FROM test_case tc JOIN test_group tg ON tc.group_id = tg.id "
                        + "WHERE tc.group_id = ? AND tg.user_id = ? AND tg.channel_id = ? ORDER BY tc.id ASC",
                this::mapCase,
                groupId,
                userId,
                channelId);
    }

    private TestGroupDto mapGroup(ResultSet rs, int rowNum) throws SQLException {
        TestGroupDto group = new TestGroupDto();
        group.setId(rs.getLong("id"));
        group.setUserId(rs.getLong("user_id"));
        group.setChannelId(rs.getString("channel_id"));
        group.setName(rs.getString("name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        group.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        group.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
        return group;
    }

    private TestCaseDto findCase(long caseId) {
        return jdbcTemplate.queryForObject("SELECT * FROM test_case WHERE id = ?", this::mapCase, caseId);
    }

    private TestCaseDto mapCase(ResultSet rs, int rowNum) throws SQLException {
        TestCaseDto testCase = new TestCaseDto();
        testCase.setId(rs.getLong("id"));
        testCase.setGroupId(rs.getLong("group_id"));
        testCase.setName(rs.getString("name"));
        testCase.setEnabled(rs.getBoolean("enabled"));
        testCase.setRequestXml(rs.getString("request_xml"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        testCase.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        testCase.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
        return testCase;
    }
}
