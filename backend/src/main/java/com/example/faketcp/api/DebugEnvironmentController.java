package com.example.faketcp.api;

import com.example.faketcp.dto.DebugEnvironmentDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.repository.DebugEnvironmentRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/debug-environments")
public class DebugEnvironmentController {
    private final DebugEnvironmentRepository repository;

    public DebugEnvironmentController(DebugEnvironmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<DebugEnvironmentDto> list(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId) {
        ApiSecurity.requireSelf(currentUser, userId);
        return repository.findByUser(userId);
    }

    @PostMapping
    public DebugEnvironmentDto create(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @RequestBody DebugEnvironmentDto environment) {
        ApiSecurity.requireSelf(currentUser, userId);
        return repository.create(userId, environment);
    }

    @PutMapping("/{environmentId}")
    public DebugEnvironmentDto update(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String environmentId,
            @RequestBody DebugEnvironmentDto environment) {
        ApiSecurity.requireSelf(currentUser, userId);
        return repository.update(userId, environmentId, environment);
    }

    @DeleteMapping("/{environmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String environmentId) {
        ApiSecurity.requireSelf(currentUser, userId);
        repository.delete(userId, environmentId);
    }
}
