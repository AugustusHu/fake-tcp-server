package com.example.faketcp.service;

import com.example.faketcp.dto.LoginRequest;
import com.example.faketcp.dto.LoginResponse;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final UserRepository userRepository;
    private final ChannelService channelService;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, ChannelService channelService) {
        this.userRepository = userRepository;
        this.channelService = channelService;
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        Optional<UserDto> authenticatedUser = userRepository
                .findByCredentials(request == null ? null : request.getUsername(), request == null ? null : request.getPassword());
        if (!authenticatedUser.isPresent()) {
            return Optional.empty();
        }
        UserDto user = authenticatedUser.get();
        String token = "web_" + UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(user, Instant.now().plus(SESSION_TTL)));
        return Optional.of(new LoginResponse(token, user));
    }

    public Optional<UserDto> resolve(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt.isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.user);
    }

    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token != null) {
            sessions.remove(token);
        }
    }

    public UserDto updateLastChannel(String authorizationHeader, UserDto currentUser, String channelId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Current user is required");
        }
        if (channelId != null && !channelId.trim().isEmpty()) {
            channelService.getRequired(channelId.trim());
        }
        UserDto updatedUser = userRepository.updateLastChannel(currentUser.getId(), channelId);
        String token = extractToken(authorizationHeader);
        if (token != null) {
            Session session = sessions.get(token);
            if (session != null) {
                sessions.put(token, new Session(updatedUser, session.expiresAt));
            }
        }
        return updatedUser;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static class Session {
        private final UserDto user;
        private final Instant expiresAt;

        private Session(UserDto user, Instant expiresAt) {
            this.user = user;
            this.expiresAt = expiresAt;
        }
    }
}
