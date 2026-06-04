package com.example.faketcp.api;

import com.example.faketcp.dto.ChannelConfigDto;
import com.example.faketcp.dto.ChannelDto;
import com.example.faketcp.dto.PackagerPreviewDto;
import com.example.faketcp.service.ChannelService;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {
    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    public List<ChannelDto> list() {
        return channelService.list();
    }

    @GetMapping("/{channelId}/packager-preview")
    public PackagerPreviewDto previewPackager(@PathVariable String channelId) {
        return channelService.previewPackager(channelId);
    }

    @PostMapping("/packager-preview")
    public PackagerPreviewDto previewDraftPackager(@RequestBody ChannelConfigDto channel) {
        return channelService.previewPackager(channel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelDto create(@Valid @RequestBody ChannelConfigDto channel) {
        return channelService.create(channel);
    }

    @PutMapping("/{channelId}")
    public ChannelDto update(@PathVariable String channelId, @Valid @RequestBody ChannelConfigDto channel) {
        return channelService.update(channelId, channel);
    }

    @DeleteMapping("/{channelId}")
    public Map<String, Object> delete(@PathVariable String channelId) {
        channelService.delete(channelId);
        Map<String, Object> effect = new HashMap<>();
        effect.put("restartRequired", true);
        effect.put("restartReasons", Arrays.asList("删除渠道需要停止 TCP listener"));
        effect.put("restartRequiredFields", Arrays.asList("delete"));
        effect.put("hotEffectiveFields", Arrays.asList(
                "channelCode",
                "name",
                "thirdPartyTestIp",
                "thirdPartyTestPort",
                "ctmk1",
                "ctmk2",
                "mockCtmk1",
                "mockCtmk2"));
        return effect;
    }
}
