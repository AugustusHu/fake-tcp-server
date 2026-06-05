package com.example.faketcp.service;

import com.example.faketcp.config.ChannelProperties;
import com.example.faketcp.dto.KeySettingsDto;
import com.example.faketcp.repository.KeySettingsRepository;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeySettingsServiceTest {
    private static final String CHANNEL_ID = "channel-a";

    @Test
    void patchValidatesOnlyChangedFields() {
        ChannelService channelService = mock(ChannelService.class);
        KeySettingsRepository repository = mock(KeySettingsRepository.class);
        KeySettingsService service = new KeySettingsService(channelService, repository);
        KeySettingsDto saved = new KeySettingsDto();
        saved.setTskPlain("419F7D9CA53CD12B1199A92700630084");
        saved.setTestDe14("BAD");

        when(channelService.getRequired(eq(CHANNEL_ID))).thenReturn(new ChannelProperties());
        when(repository.patch(eq(CHANNEL_ID), eq(Collections.singletonMap("tskPlain", "419F7D9CA53CD12B1199A92700630084"))))
                .thenReturn(saved);

        KeySettingsDto result = service.patch(CHANNEL_ID, Collections.singletonMap("tskPlain", "419F7D9CA53CD12B1199A92700630084"));

        assertThat(result.getTskPlain()).isEqualTo("419F7D9CA53CD12B1199A92700630084");
    }
}
