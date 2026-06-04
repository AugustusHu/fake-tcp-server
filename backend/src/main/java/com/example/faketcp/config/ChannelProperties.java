package com.example.faketcp.config;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

public class ChannelProperties {
    @NotBlank
    private String id;
    @NotBlank
    private String name;
    private boolean enabled = true;
    @Valid
    private TcpProperties tcp = new TcpProperties();
    @Valid
    private FramingProperties framing = new FramingProperties();
    @Valid
    private HeaderProperties header = new HeaderProperties();
    @Valid
    private Iso8583Properties iso8583 = new Iso8583Properties();
    @Valid
    private NoMatchProperties noMatch = new NoMatchProperties();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TcpProperties getTcp() {
        return tcp;
    }

    public void setTcp(TcpProperties tcp) {
        this.tcp = tcp;
    }

    public FramingProperties getFraming() {
        return framing;
    }

    public void setFraming(FramingProperties framing) {
        this.framing = framing;
    }

    public HeaderProperties getHeader() {
        return header;
    }

    public void setHeader(HeaderProperties header) {
        this.header = header;
    }

    public Iso8583Properties getIso8583() {
        return iso8583;
    }

    public void setIso8583(Iso8583Properties iso8583) {
        this.iso8583 = iso8583;
    }

    public NoMatchProperties getNoMatch() {
        return noMatch;
    }

    public void setNoMatch(NoMatchProperties noMatch) {
        this.noMatch = noMatch;
    }
}
