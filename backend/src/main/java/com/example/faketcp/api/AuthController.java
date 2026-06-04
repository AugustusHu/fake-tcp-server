package com.example.faketcp.api;

import com.example.faketcp.dto.LoginRequest;
import com.example.faketcp.dto.LoginResponse;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.service.AuthService;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<LoginResponse> response = authService.login(request);
        if (!response.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "用户名或密码错误"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response.get());
    }

    @GetMapping("/me")
    public UserDto me(@RequestAttribute("currentUser") UserDto currentUser) {
        return currentUser;
    }

    @PutMapping("/me/last-channel")
    public UserDto updateLastChannel(
            @RequestAttribute("currentUser") UserDto currentUser,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, String> body) {
        String channelId = body == null ? null : body.get("channelId");
        return authService.updateLastChannel(authorization, currentUser, channelId);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
    }
}
