package com.example.faketcp.api;

import com.example.faketcp.dto.TestCaseDto;
import com.example.faketcp.dto.TestGroupDto;
import com.example.faketcp.dto.TestRunResultDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.service.TestGroupService;
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
@RequestMapping("/api/users/{userId}/channels/{channelId}/test-groups")
public class TestGroupController {
    private final TestGroupService service;

    public TestGroupController(TestGroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<TestGroupDto> list(@RequestAttribute("currentUser") UserDto currentUser, @PathVariable long userId, @PathVariable String channelId) {
        ApiSecurity.requireSelf(currentUser, userId);
        return service.list(userId, channelId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestGroupDto createGroup(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId,
            @RequestBody TestGroupDto group) {
        ApiSecurity.requireSelf(currentUser, userId);
        return service.createGroup(userId, channelId, group);
    }

    @PostMapping("/{groupId}/cases")
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseDto createCase(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId,
            @PathVariable long groupId,
            @RequestBody TestCaseDto testCase) {
        ApiSecurity.requireSelf(currentUser, userId);
        return service.createCase(userId, channelId, groupId, testCase);
    }

    @PostMapping("/{groupId}/run")
    public List<TestRunResultDto> runGroup(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId,
            @PathVariable long groupId) {
        ApiSecurity.requireSelf(currentUser, userId);
        return service.runGroup(userId, channelId, groupId, currentUser);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId,
            @PathVariable long groupId) {
        ApiSecurity.requireSelf(currentUser, userId);
        service.deleteGroup(userId, channelId, groupId);
    }

    @DeleteMapping("/{groupId}/cases/{caseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCase(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable long userId,
            @PathVariable String channelId,
            @PathVariable long groupId,
            @PathVariable long caseId) {
        ApiSecurity.requireSelf(currentUser, userId);
        service.deleteCase(userId, channelId, groupId, caseId);
    }
}
