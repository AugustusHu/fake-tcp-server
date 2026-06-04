package com.example.faketcp.repository;

import com.example.faketcp.dto.McpTokenDto;
import com.example.faketcp.dto.UserDto;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private static final String DEFAULT_PASSWORD = "123456";
    private static final int MAX_ACTIVE_MCP_TOKENS = 3;
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserDto> findAll() {
        return jdbcTemplate.query("SELECT * FROM app_user ORDER BY id ASC", this::mapUser);
    }

    public Optional<UserDto> findByCredentials(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        List<UserDto> users = jdbcTemplate.query(
                "SELECT * FROM app_user WHERE username = ? AND password_hash = ?",
                this::mapUser,
                username.trim(),
                hash(password));
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public UserDto create(UserDto user) {
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        if (username.isEmpty()) {
            throw new IllegalStateException("用户名不能为空");
        }
        if (existsByUsername(username)) {
            throw new IllegalStateException("用户名已存在");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO app_user (username, display_name, password_hash) VALUES (?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, username);
            ps.setString(2, user.getDisplayName() == null || user.getDisplayName().trim().isEmpty()
                    ? username
                    : user.getDisplayName());
            ps.setString(3, hash(DEFAULT_PASSWORD));
            return ps;
        }, keyHolder);
        return findById(keyHolder.getKey().longValue());
    }

    public UserDto findById(long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM app_user WHERE id = ?", this::mapUser, id);
    }

    public UserDto updateLastChannel(long userId, String channelId) {
        findById(userId);
        jdbcTemplate.update(
                "UPDATE app_user SET last_channel_id = ? WHERE id = ?",
                channelId == null || channelId.trim().isEmpty() ? null : channelId.trim(),
                userId);
        return findById(userId);
    }

    public Optional<UserDto> findByActiveMcpToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        List<UserDto> users = jdbcTemplate.query(
                "SELECT u.* FROM app_user u JOIN mcp_token t ON u.id = t.user_id "
                        + "WHERE t.token = ? AND t.revoked = FALSE",
                this::mapUser,
                token.trim());
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE username = ?",
                Integer.class,
                username == null ? "" : username.trim());
        return count != null && count > 0;
    }

    public McpTokenDto createToken(long userId, String name) {
        findById(userId);
        if (countActiveTokens(userId) >= MAX_ACTIVE_MCP_TOKENS) {
            throw new IllegalStateException("每个用户最多只能创建 3 个有效 MCP Token");
        }
        String token = "mcp_" + UUID.randomUUID().toString().replace("-", "");
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO mcp_token (user_id, name, token) VALUES (?, ?, ?)",
                    new String[]{"id"});
            ps.setLong(1, userId);
            ps.setString(2, name == null || name.trim().isEmpty() ? "MCP token" : name);
            ps.setString(3, token);
            return ps;
        }, keyHolder);
        return findToken(keyHolder.getKey().longValue());
    }

    public List<McpTokenDto> findTokens(long userId) {
        findById(userId);
        return jdbcTemplate.query(
                "SELECT * FROM mcp_token WHERE user_id = ? ORDER BY created_at DESC",
                this::mapToken,
                userId);
    }

    public int countActiveTokens(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mcp_token WHERE user_id = ? AND revoked = FALSE",
                Integer.class,
                userId);
        return count == null ? 0 : count;
    }

    public void revokeToken(long userId, long tokenId) {
        jdbcTemplate.update("UPDATE mcp_token SET revoked = TRUE WHERE user_id = ? AND id = ?", userId, tokenId);
    }

    private McpTokenDto findToken(long tokenId) {
        return jdbcTemplate.queryForObject("SELECT * FROM mcp_token WHERE id = ?", this::mapToken, tokenId);
    }

    private UserDto mapUser(ResultSet rs, int rowNum) throws SQLException {
        UserDto user = new UserDto();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setDisplayName(rs.getString("display_name"));
        user.setAdmin("admin".equalsIgnoreCase(user.getUsername()));
        user.setLastChannelId(rs.getString("last_channel_id"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        user.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        return user;
    }

    private McpTokenDto mapToken(ResultSet rs, int rowNum) throws SQLException {
        McpTokenDto token = new McpTokenDto();
        token.setId(rs.getLong("id"));
        token.setUserId(rs.getLong("user_id"));
        token.setName(rs.getString("name"));
        token.setToken(rs.getString("token"));
        token.setRevoked(rs.getBoolean("revoked"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        token.setCreatedAt(createdAt == null ? null : createdAt.toInstant());
        return token;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash password", e);
        }
    }
}
