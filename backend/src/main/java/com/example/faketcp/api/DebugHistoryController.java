package com.example.faketcp.api;

import com.example.faketcp.dto.DebugHistoryDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.repository.DebugHistoryRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/debug-history")
public class DebugHistoryController {
    private final DebugHistoryRepository repository;

    public DebugHistoryController(DebugHistoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<DebugHistoryDto> list(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @RequestParam String environmentId) {
        ApiSecurity.requireSelf(currentUser, userId);
        return repository.findRecent(userId, environmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DebugHistoryDto create(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @RequestBody DebugHistoryDto history) {
        ApiSecurity.requireSelf(currentUser, userId);
        return repository.create(userId, history);
    }

    @DeleteMapping("/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @RequestParam String environmentId,
            @PathVariable long historyId) {
        ApiSecurity.requireSelf(currentUser, userId);
        repository.delete(userId, environmentId, historyId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @RequestParam String environmentId) {
        ApiSecurity.requireSelf(currentUser, userId);
        repository.clear(userId, environmentId);
    }
}
