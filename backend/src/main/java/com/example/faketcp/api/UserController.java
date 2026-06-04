package com.example.faketcp.api;

import com.example.faketcp.dto.McpTokenDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserDto> list(@RequestAttribute("currentUser") UserDto currentUser) {
        ApiSecurity.requireAdmin(currentUser);
        return userRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestAttribute("currentUser") UserDto currentUser, @RequestBody UserDto user) {
        ApiSecurity.requireAdmin(currentUser);
        return userRepository.create(user);
    }

    @GetMapping("/{userId}/tokens")
    public List<McpTokenDto> tokens(@RequestAttribute("currentUser") UserDto currentUser, @PathVariable long userId) {
        ApiSecurity.requireSelfOrAdmin(currentUser, userId);
        return userRepository.findTokens(userId);
    }

    @PostMapping("/{userId}/tokens")
    @ResponseStatus(HttpStatus.CREATED)
    public McpTokenDto createToken(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @RequestBody(required = false) Map<String, String> body) {
        ApiSecurity.requireSelfOrAdmin(currentUser, userId);
        return userRepository.createToken(userId, body == null ? null : body.get("name"));
    }

    @DeleteMapping("/{userId}/tokens/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeToken(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable long tokenId) {
        ApiSecurity.requireSelfOrAdmin(currentUser, userId);
        userRepository.revokeToken(userId, tokenId);
    }
}
