package com.example.faketcp.api;

import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.service.KeySettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels/{channelId}/keys")
public class KeySettingsController {
    private final KeySettingsService keySettingsService;

    public KeySettingsController(KeySettingsService keySettingsService) {
        this.keySettingsService = keySettingsService;
    }

    @GetMapping
    public KeySettingsDto get(@PathVariable String channelId) {
        return keySettingsService.get(channelId);
    }

    @PutMapping
    public KeySettingsDto save(@PathVariable String channelId, @RequestBody KeySettingsDto settings) {
        return keySettingsService.save(channelId, settings);
    }
}
