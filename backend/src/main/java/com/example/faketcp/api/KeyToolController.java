package com.example.faketcp.api;

import com.example.faketcp.dto.KeyDecryptRequestDto;
import com.example.faketcp.dto.KeyDecryptResponseDto;
import com.example.faketcp.service.KeyDecryptToolService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels/{channelId}/tools")
public class KeyToolController {
    private final KeyDecryptToolService keyDecryptToolService;

    public KeyToolController(KeyDecryptToolService keyDecryptToolService) {
        this.keyDecryptToolService = keyDecryptToolService;
    }

    @PostMapping("/key-decrypt")
    public KeyDecryptResponseDto decrypt(@PathVariable String channelId, @RequestBody(required = false) KeyDecryptRequestDto request) {
        return keyDecryptToolService.decrypt(channelId, request);
    }
}
