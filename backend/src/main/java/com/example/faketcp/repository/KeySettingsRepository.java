package com.example.faketcp.repository;

import com.example.faketcp.dto.KeySettingsDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KeySettingsRepository {
    private final JdbcTemplate jdbcTemplate;

    public KeySettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public KeySettingsDto findByChannel(String channelId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM channel_key_setting WHERE channel_id = ?",
                    this::mapSettings,
                    channelId);
        } catch (EmptyResultDataAccessException e) {
            KeySettingsDto settings = new KeySettingsDto();
            settings.setChannelId(channelId);
            return settings;
        }
    }

    public KeySettingsDto save(String channelId, KeySettingsDto settings) {
        jdbcTemplate.update(
                "INSERT INTO channel_key_setting "
                        + "(channel_id, tpk_plain, tsk_plain, mac_field, mac_algorithm, test_tid, test_pan, test_pin, "
                        + "test_de14, test_de42, test_de18, test_de43, test_de49) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE tpk_plain = VALUES(tpk_plain), tsk_plain = VALUES(tsk_plain), "
                        + "mac_field = VALUES(mac_field), mac_algorithm = VALUES(mac_algorithm), "
                        + "test_tid = VALUES(test_tid), test_pan = VALUES(test_pan), test_pin = VALUES(test_pin), "
                        + "test_de14 = VALUES(test_de14), test_de42 = VALUES(test_de42), test_de18 = VALUES(test_de18), test_de43 = VALUES(test_de43), "
                        + "test_de49 = VALUES(test_de49)",
                channelId,
                blankToNull(settings.getTpkPlain()),
                blankToNull(settings.getTskPlain()),
                blankToDefault(settings.getMacField(), "128"),
                blankToDefault(settings.getMacAlgorithm(), "ANSI_X9_19"),
                blankToNull(settings.getTestTid()),
                blankToNull(settings.getTestPan()),
                blankToNull(settings.getTestPin()),
                blankToNull(settings.getTestDe14()),
                blankToNull(settings.getTestDe42()),
                blankToNull(settings.getTestDe18()),
                blankToNull(settings.getTestDe43()),
                blankToNull(settings.getTestDe49()));
        return findByChannel(channelId);
    }

    private KeySettingsDto mapSettings(ResultSet rs, int rowNum) throws SQLException {
        KeySettingsDto settings = new KeySettingsDto();
        settings.setChannelId(rs.getString("channel_id"));
        settings.setTpkPlain(rs.getString("tpk_plain"));
        settings.setTskPlain(rs.getString("tsk_plain"));
        settings.setMacField(rs.getString("mac_field"));
        settings.setMacAlgorithm(rs.getString("mac_algorithm"));
        settings.setTestTid(rs.getString("test_tid"));
        settings.setTestPan(rs.getString("test_pan"));
        settings.setTestPin(rs.getString("test_pin"));
        settings.setTestDe14(rs.getString("test_de14"));
        settings.setTestDe42(rs.getString("test_de42"));
        settings.setTestDe18(rs.getString("test_de18"));
        settings.setTestDe43(rs.getString("test_de43"));
        settings.setTestDe49(rs.getString("test_de49"));
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        settings.setUpdatedAt(updatedAt == null ? null : updatedAt.toInstant());
        return settings;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
