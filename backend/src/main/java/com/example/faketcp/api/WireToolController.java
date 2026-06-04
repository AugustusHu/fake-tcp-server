package com.example.faketcp.api;

import com.example.faketcp.dto.WireParseRequestDto;
import com.example.faketcp.dto.WireParseResponseDto;
import com.example.faketcp.dto.WireRequestDto;
import com.example.faketcp.dto.WireRequestPreviewDto;
import com.example.faketcp.service.WireToolService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels/{channelId}/tools")
public class WireToolController {
    private final WireToolService wireToolService;

    public WireToolController(WireToolService wireToolService) {
        this.wireToolService = wireToolService;
    }

    @PostMapping("/serialize")
    public WireRequestPreviewDto serializeRequest(@PathVariable String channelId, @RequestBody WireRequestDto request) {
        return wireToolService.serializeRequest(channelId, request.getRequestXml());
    }

    @PostMapping("/build-request")
    public WireRequestPreviewDto buildRequest(@PathVariable String channelId, @RequestBody WireRequestDto request) {
        return wireToolService.serializeRequest(channelId, request.getRequestXml());
    }

    @PostMapping("/deserialize")
    public WireParseResponseDto deserializeResponse(@PathVariable String channelId, @RequestBody WireParseRequestDto request) {
        return wireToolService.deserializeResponse(channelId, request.getResponseHex());
    }

    @PostMapping("/parse-response")
    public WireParseResponseDto parseResponse(@PathVariable String channelId, @RequestBody WireParseRequestDto request) {
        return wireToolService.deserializeResponse(channelId, request.getResponseHex());
    }
}
