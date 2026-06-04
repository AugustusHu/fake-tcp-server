package com.example.faketcp.api;

import com.example.faketcp.dto.IsoMessageDto;
import com.example.faketcp.dto.RuleCapability;
import com.example.faketcp.dto.RuleDto;
import com.example.faketcp.dto.TestMatchResponse;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.service.RuleService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels/{channelId}/rules")
public class RuleController {
    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public List<RuleDto> list(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @RequestParam(required = false) RuleCapability capability) {
        return ruleService.list(channelId, capability, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RuleDto create(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @RequestBody RuleDto rule) {
        return ruleService.create(channelId, rule, currentUser);
    }

    @PutMapping("/{ruleId}")
    public RuleDto update(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @PathVariable long ruleId,
            @RequestBody RuleDto rule) {
        return ruleService.update(channelId, ruleId, rule, currentUser);
    }

    @PostMapping("/{ruleId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @PathVariable long ruleId) {
        ruleService.setEnabled(channelId, ruleId, true, currentUser);
    }

    @PostMapping("/{ruleId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @PathVariable long ruleId) {
        ruleService.setEnabled(channelId, ruleId, false, currentUser);
    }

    @PostMapping("/{ruleId}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publish(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @PathVariable long ruleId) {
        ruleService.setPublicRule(channelId, ruleId, true, currentUser);
    }

    @PostMapping("/{ruleId}/unpublish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpublish(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @PathVariable long ruleId) {
        ruleService.setPublicRule(channelId, ruleId, false, currentUser);
    }

    @GetMapping("/public")
    public List<RuleDto> publicRules() {
        return ruleService.publicRules();
    }

    @PostMapping("/public/{ruleId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleDto copyPublicRule(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @PathVariable long ruleId) {
        return ruleService.copyPublicRule(channelId, ruleId, currentUser);
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @PathVariable long ruleId) {
        ruleService.delete(channelId, ruleId, currentUser);
    }

    @PostMapping("/test")
    public TestMatchResponse test(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @RequestBody IsoMessageDto request) {
        return ruleService.test(channelId, request, currentUser);
    }

    @PostMapping(
            value = "/test-xml",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = MediaType.APPLICATION_XML_VALUE)
    public String testXml(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @RequestBody String requestXml) {
        return ruleService.testXml(channelId, requestXml, currentUser);
    }
}
