package com.example.faketcp.api;

import com.example.faketcp.dto.RequestLogDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.repository.RequestLogRepository;
import com.example.faketcp.service.ChannelService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels/{channelId}/logs")
public class RequestLogController {
    private final RequestLogRepository requestLogRepository;
    private final ChannelService channelService;

    public RequestLogController(RequestLogRepository requestLogRepository, ChannelService channelService) {
        this.requestLogRepository = requestLogRepository;
        this.channelService = channelService;
    }

    @GetMapping
    public List<RequestLogDto> list(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @RequestParam(defaultValue = "100") int limit) {
        ApiSecurity.requireAdmin(currentUser);
        channelService.getRequired(channelId);
        return requestLogRepository.findRecent(channelId, Math.min(limit, 500));
    }

    @GetMapping("/{logId}")
    public RequestLogDto get(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @PathVariable long logId) {
        ApiSecurity.requireAdmin(currentUser);
        channelService.getRequired(channelId);
        return requestLogRepository.findById(channelId, logId);
    }
}
