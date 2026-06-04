package com.example.faketcp.api;

import com.example.faketcp.dto.PosDebugBuildXmlRequestDto;
import com.example.faketcp.dto.PosDebugBuildXmlResponseDto;
import com.example.faketcp.dto.PosDebugResponseDto;
import com.example.faketcp.dto.PosDebugSendRequestDto;
import com.example.faketcp.dto.PosDebugTidInitRequestDto;
import com.example.faketcp.dto.UserDto;
import com.example.faketcp.service.PosDebugService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels/{channelId}/debug/pos")
public class DebugController {
    private final PosDebugService posDebugService;

    public DebugController(PosDebugService posDebugService) {
        this.posDebugService = posDebugService;
    }

    @PostMapping("/build-xml")
    public PosDebugBuildXmlResponseDto buildXml(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @RequestBody PosDebugBuildXmlRequestDto request) {
        return posDebugService.buildXml(currentUser.getId(), channelId, request);
    }

    @PostMapping("/send")
    public PosDebugResponseDto send(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @RequestBody PosDebugSendRequestDto request) {
        return posDebugService.send(currentUser.getId(), channelId, request);
    }

    @PostMapping("/tid-init")
    public PosDebugResponseDto tidInit(
            @RequestAttribute("currentUser") UserDto currentUser,
            @PathVariable String channelId,
            @RequestBody PosDebugTidInitRequestDto request) {
        return posDebugService.tidInit(currentUser.getId(), channelId, request);
    }
}
