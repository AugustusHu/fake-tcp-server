package com.example.faketcp.api;

import com.example.faketcp.dto.UserDto;
import com.example.faketcp.repository.UserRepository;
import com.example.faketcp.service.McpService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpController {
    private final McpService mcpService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public McpController(McpService mcpService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.mcpService = mcpService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Object body) {
        Optional<UserDto> user = userRepository.findByActiveMcpToken(extractBearerToken(authorization));
        if (!user.isPresent()) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(mcpService.error(idFrom(body), -32001, "Invalid or revoked MCP token"));
        }
        if (body instanceof List) {
            List<?> batch = (List<?>) body;
            List<Map<String, Object>> responses = new ArrayList<>();
            for (Object item : batch) {
                Map<String, Object> response = mcpService.handle(toMap(item), user.get());
                if (response != null) {
                    responses.add(response);
                }
            }
            return responses.isEmpty() ? ResponseEntity.accepted().build() : ResponseEntity.ok(responses);
        }
        Map<String, Object> response = mcpService.handle(toMap(body), user.get());
        return response == null ? ResponseEntity.accepted().build() : ResponseEntity.ok(response);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private Map<String, Object> toMap(Object value) {
        return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
    }

    private Object idFrom(Object body) {
        if (body instanceof Map) {
            return ((Map<?, ?>) body).get("id");
        }
        if (body instanceof List && !((List<?>) body).isEmpty()) {
            Object first = ((List<?>) body).get(0);
            if (first instanceof Map) {
                return ((Map<?, ?>) first).get("id");
            }
        }
        return null;
    }
}
