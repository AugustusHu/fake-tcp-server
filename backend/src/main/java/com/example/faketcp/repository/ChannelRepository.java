package com.example.faketcp.repository;

import com.example.faketcp.dto.ChannelConfigDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelRepository {
    private final JdbcTemplate jdbcTemplate;

    public ChannelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ChannelConfigDto> findAll() {
        return jdbcTemplate.query("SELECT * FROM channel_config ORDER BY id", this::mapChannel);
    }

    public ChannelConfigDto findById(String id) {
        return jdbcTemplate.queryForObject("SELECT * FROM channel_config WHERE id = ?", this::mapChannel, id);
    }

    public boolean existsById(String id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM channel_config WHERE id = ?",
                Integer.class,
                trimToEmpty(id));
        return count != null && count > 0;
    }

    public boolean existsByCode(String channelCode, String excludedId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM channel_config WHERE channel_code = ? AND id <> ?",
                Integer.class,
                trimToEmpty(channelCode),
                trimToEmpty(excludedId));
        return count != null && count > 0;
    }

    public boolean existsByPort(int port, String excludedId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM channel_config WHERE port = ? AND id <> ?",
                Integer.class,
                port,
                trimToEmpty(excludedId));
        return count != null && count > 0;
    }

    public ChannelConfigDto create(ChannelConfigDto channel) {
        if (isBlank(channel.getId())) {
            channel.setId(generateId());
        }
        channel.setName(channel.getChannelCode());
        jdbcTemplate.update(
                "INSERT INTO channel_config (id, channel_code, name, enabled, host, port, framing_type, byte_order, length_includes, "
                        + "header_enabled, header_length, header_response_mode, header_fixed_value_hex, packager_type, "
                        + "packager_config_mode, packager_location, packager_file_name, packager_content, packager_class_name, "
                        + "no_match_response_code, third_party_test_ip, third_party_test_port, third_party_tls_enabled, mock_tls_enabled, "
                        + "mock_ctmk1, mock_ctmk2, ctmk1, ctmk2) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                channel.getId(),
                channel.getChannelCode(),
                channel.getName(),
                channel.isEnabled(),
                channel.getHost(),
                channel.getPort(),
                channel.getFramingType(),
                channel.getByteOrder(),
                channel.getLengthIncludes(),
                channel.isHeaderEnabled(),
                channel.getHeaderLength(),
                channel.getHeaderResponseMode(),
                channel.getHeaderFixedValueHex(),
                channel.getPackagerType(),
                channel.getPackagerConfigMode(),
                channel.getPackagerLocation(),
                channel.getPackagerFileName(),
                channel.getPackagerContent(),
                channel.getPackagerClassName(),
                channel.getNoMatchResponseCode(),
                blankToNull(channel.getThirdPartyTestIp()),
                channel.getThirdPartyTestPort(),
                channel.isThirdPartyTlsEnabled(),
                channel.getMockTlsEnabled(),
                blankToNull(channel.getMockCtmk1()),
                blankToNull(channel.getMockCtmk2()),
                blankToNull(channel.getCtmk1()),
                blankToNull(channel.getCtmk2()));
        return findById(channel.getId());
    }

    public ChannelConfigDto update(String id, ChannelConfigDto channel) {
        channel.setName(channel.getChannelCode());
        jdbcTemplate.update(
                "UPDATE channel_config SET channel_code = ?, name = ?, enabled = ?, host = ?, port = ?, framing_type = ?, byte_order = ?, "
                        + "length_includes = ?, header_enabled = ?, header_length = ?, header_response_mode = ?, "
                        + "header_fixed_value_hex = ?, packager_type = ?, packager_config_mode = ?, packager_location = ?, "
                        + "packager_file_name = ?, packager_content = ?, packager_class_name = ?, no_match_response_code = ?, "
                        + "third_party_test_ip = ?, third_party_test_port = ?, third_party_tls_enabled = ?, mock_tls_enabled = ?, "
                        + "mock_ctmk1 = ?, mock_ctmk2 = ?, ctmk1 = ?, ctmk2 = ? "
                        + "WHERE id = ?",
                channel.getChannelCode(),
                channel.getName(),
                channel.isEnabled(),
                channel.getHost(),
                channel.getPort(),
                channel.getFramingType(),
                channel.getByteOrder(),
                channel.getLengthIncludes(),
                channel.isHeaderEnabled(),
                channel.getHeaderLength(),
                channel.getHeaderResponseMode(),
                channel.getHeaderFixedValueHex(),
                channel.getPackagerType(),
                channel.getPackagerConfigMode(),
                channel.getPackagerLocation(),
                channel.getPackagerFileName(),
                channel.getPackagerContent(),
                channel.getPackagerClassName(),
                channel.getNoMatchResponseCode(),
                blankToNull(channel.getThirdPartyTestIp()),
                channel.getThirdPartyTestPort(),
                channel.isThirdPartyTlsEnabled(),
                channel.getMockTlsEnabled(),
                blankToNull(channel.getMockCtmk1()),
                blankToNull(channel.getMockCtmk2()),
                blankToNull(channel.getCtmk1()),
                blankToNull(channel.getCtmk2()),
                id);
        return findById(id);
    }

    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM channel_config WHERE id = ?", id);
    }

    private ChannelConfigDto mapChannel(ResultSet rs, int rowNum) throws SQLException {
        ChannelConfigDto channel = new ChannelConfigDto();
        channel.setId(rs.getString("id"));
        channel.setChannelCode(rs.getString("channel_code"));
        channel.setName(rs.getString("name"));
        channel.setEnabled(rs.getBoolean("enabled"));
        channel.setHost(rs.getString("host"));
        channel.setPort(rs.getInt("port"));
        channel.setFramingType(rs.getString("framing_type"));
        channel.setByteOrder(rs.getString("byte_order"));
        channel.setLengthIncludes(rs.getString("length_includes"));
        channel.setHeaderEnabled(rs.getBoolean("header_enabled"));
        channel.setHeaderLength(rs.getInt("header_length"));
        channel.setHeaderResponseMode(rs.getString("header_response_mode"));
        channel.setHeaderFixedValueHex(rs.getString("header_fixed_value_hex"));
        channel.setPackagerType(rs.getString("packager_type"));
        channel.setPackagerConfigMode(rs.getString("packager_config_mode"));
        channel.setPackagerLocation(rs.getString("packager_location"));
        channel.setPackagerFileName(rs.getString("packager_file_name"));
        channel.setPackagerContent(rs.getString("packager_content"));
        channel.setPackagerClassName(rs.getString("packager_class_name"));
        channel.setNoMatchResponseCode(rs.getString("no_match_response_code"));
        channel.setThirdPartyTestIp(rs.getString("third_party_test_ip"));
        int thirdPartyTestPort = rs.getInt("third_party_test_port");
        channel.setThirdPartyTestPort(rs.wasNull() ? null : thirdPartyTestPort);
        channel.setThirdPartyTlsEnabled(rs.getBoolean("third_party_tls_enabled"));
        boolean mockTlsEnabled = rs.getBoolean("mock_tls_enabled");
        channel.setMockTlsEnabled(rs.wasNull() ? null : mockTlsEnabled);
        channel.setMockCtmk1(rs.getString("mock_ctmk1"));
        channel.setMockCtmk2(rs.getString("mock_ctmk2"));
        channel.setCtmk1(rs.getString("ctmk1"));
        channel.setCtmk2(rs.getString("ctmk2"));
        return channel;
    }

    private String generateId() {
        return "ch_" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
