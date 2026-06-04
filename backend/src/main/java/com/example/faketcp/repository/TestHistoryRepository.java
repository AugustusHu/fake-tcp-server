package com.example.faketcp.repository;

import com.example.faketcp.dto.TestHistoryDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TestHistoryRepository {
    private static final int MAX_HISTORY_PER_SCOPE = 30;
    private final JdbcTemplate jdbcTemplate;

    public TestHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TestHistoryDto> findRecent(long userId, String channelId) {
        return jdbcTemplate.query(
                "SELECT * FROM test_history WHERE user_id = ? AND channel_id = ? ORDER BY created_at DESC, id DESC LIMIT ?",
                this::map,
                userId,
                channelId,
                MAX_HISTORY_PER_SCOPE);
    }

    public TestHistoryDto create(TestHistoryDto history) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO test_history "
                            + "(user_id, channel_id, success, matched, rule_id, rule_name, request_xml, response_xml, error_message) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setLong(1, history.getUserId());
            ps.setString(2, history.getChannelId());
            ps.setBoolean(3, history.isSuccess());
            ps.setBoolean(4, history.isMatched());
            if (history.getRuleId() == null) {
                ps.setNull(5, java.sql.Types.BIGINT);
            } else {
                ps.setLong(5, history.getRuleId());
            }
            ps.setString(6, history.getRuleName());
            ps.setString(7, history.getRequestXml());
            ps.setString(8, history.getResponseXml());
            ps.setString(9, history.getErrorMessage());
            return ps;
        }, keyHolder);
        trim(history.getUserId(), history.getChannelId());
        return findOne(keyHolder.getKey().longValue());
    }

    public void delete(long userId, String channelId, long historyId) {
        jdbcTemplate.update(
                "DELETE FROM test_history WHERE id = ? AND user_id = ? AND channel_id = ?",
                historyId,
                userId,
                channelId);
    }

    public void clear(long userId, String channelId) {
        jdbcTemplate.update("DELETE FROM test_history WHERE user_id = ? AND channel_id = ?", userId, channelId);
    }

    private TestHistoryDto findOne(long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM test_history WHERE id = ?", this::map, id);
    }

    private void trim(long userId, String channelId) {
        jdbcTemplate.update(
                "DELETE FROM test_history "
                        + "WHERE user_id = ? AND channel_id = ? AND id NOT IN ("
                        + "  SELECT id FROM ("
                        + "    SELECT id FROM test_history WHERE user_id = ? AND channel_id = ? ORDER BY created_at DESC, id DESC LIMIT ?"
                        + "  ) recent_history"
                        + ")",
                userId,
                channelId,
                userId,
                channelId,
                MAX_HISTORY_PER_SCOPE);
    }

    private TestHistoryDto map(ResultSet rs, int rowNum) throws SQLException {
        TestHistoryDto history = new TestHistoryDto();
        history.setId(rs.getLong("id"));
        history.setUserId(rs.getLong("user_id"));
        history.setChannelId(rs.getString("channel_id"));
        history.setSuccess(rs.getBoolean("success"));
        history.setMatched(rs.getBoolean("matched"));
        long ruleId = rs.getLong("rule_id");
        history.setRuleId(rs.wasNull() ? null : ruleId);
        history.setRuleName(rs.getString("rule_name"));
        history.setRequestXml(rs.getString("request_xml"));
        history.setResponseXml(rs.getString("response_xml"));
        history.setErrorMessage(rs.getString("error_message"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        history.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        return history;
    }
}
