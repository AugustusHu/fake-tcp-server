package com.example.faketcp.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fake-tcp")
public class FakeTcpProperties {
    private List<ChannelProperties> channels = new ArrayList<>();
    private MockTlsProperties mockTls = new MockTlsProperties();

    public List<ChannelProperties> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelProperties> channels) {
        this.channels = channels;
    }

    public MockTlsProperties getMockTls() {
        return mockTls;
    }

    public void setMockTls(MockTlsProperties mockTls) {
        this.mockTls = mockTls;
    }
}
