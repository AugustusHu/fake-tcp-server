package com.example.faketcp.api;

import com.example.faketcp.dto.DebugCollectionDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.repository.DebugCollectionRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/debug-collections")
public class DebugCollectionController {
    private final DebugCollectionRepository repository;

    public DebugCollectionController(DebugCollectionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<DebugCollectionDto> list(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @RequestParam String environmentId) {
        ApiSecurity.requireSelf(currentUser, userId);
        return repository.findByEnvironment(userId, environmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DebugCollectionDto create(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @RequestBody DebugCollectionDto collection) {
        ApiSecurity.requireSelf(currentUser, userId);
        return repository.create(userId, collection);
    }

    @PutMapping("/{collectionId}")
    public DebugCollectionDto update(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String collectionId,
            @RequestBody DebugCollectionDto collection) {
        ApiSecurity.requireSelf(currentUser, userId);
        return repository.update(userId, collectionId, collection);
    }

    @DeleteMapping("/{collectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String collectionId) {
        ApiSecurity.requireSelf(currentUser, userId);
        repository.delete(userId, collectionId);
    }
}
