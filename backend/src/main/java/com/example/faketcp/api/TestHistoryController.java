package com.example.faketcp.api;

import com.example.faketcp.dto.TestHistoryDto;
import com.example.faketcp.dto.TestHistoryRunRequestDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.service.TestHistoryService;
import java.util.List;
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
@RequestMapping("/api/users/{userId}/channels/{channelId}/test-history")
public class TestHistoryController {
    private final TestHistoryService service;

    public TestHistoryController(TestHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestHistoryDto> list(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId) {
        ApiSecurity.requireSelf(currentUser, userId);
        return service.list(currentUser, channelId);
    }

    @PostMapping("/run")
    public TestHistoryDto run(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId,
            @RequestBody TestHistoryRunRequestDto request) {
        ApiSecurity.requireSelf(currentUser, userId);
        return service.run(currentUser, channelId, request);
    }

    @DeleteMapping("/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId,
            @PathVariable long historyId) {
        ApiSecurity.requireSelf(currentUser, userId);
        service.delete(userId, channelId, historyId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId) {
        ApiSecurity.requireSelf(currentUser, userId);
        service.clear(userId, channelId);
    }
}
